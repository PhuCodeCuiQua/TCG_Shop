package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.repository.OrderItemQuantity;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.service.CommissionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderStatusSideEffectHandlerTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 10, 12, 0);
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC);

	@Mock private OrderItemRepository orderItemRepository;
	@Mock private ProductRepository productRepository;
	@Mock private CommissionService commissionService;

	private OrderStatusSideEffectHandler handler;

	@BeforeEach
	void setUp() {
		handler = new OrderStatusSideEffectHandler(orderItemRepository, productRepository,
				commissionService, new OrderProperties(7), FIXED_CLOCK);
	}

	@Test
	void cancellationTrimsReasonRestoresStockInProductOrderAndDoesNotCountSales() {
		Order order = order(1L);
		order.setOrderGroup(group(PaymentMethod.COD, PaymentStatus.PENDING));
		OrderItemQuantity higherId = quantity(90L, 3L);
		OrderItemQuantity lowerId = quantity(12L, 2L);
		when(orderItemRepository.findQuantitiesByOrderId(1L)).thenReturn(List.of(lowerId, higherId));

		handler.apply(order, OrderStatus.NEW, OrderStatus.CANCELLED, "  Lý do hủy  ");

		assertEquals("Lý do hủy", order.getCancelReason());
		assertNull(order.getRefundAmount());
		assertNull(order.getRefundedAt());
		InOrder ordered = inOrder(productRepository);
		ordered.verify(productRepository).increaseStock(12L, 2);
		ordered.verify(productRepository).increaseStock(90L, 3);
		verify(productRepository, never()).increaseSoldCount(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyInt());
	}

	@Test
	void paidMomoCancellationMarksRefundWithoutChangingGroupPaymentStatus() {
		Order order = order(2L);
		OrderGroup group = group(PaymentMethod.MOMO, PaymentStatus.PAID);
		order.setOrderGroup(group);
		when(orderItemRepository.findQuantitiesByOrderId(2L)).thenReturn(List.of());

		handler.apply(order, OrderStatus.NEW, OrderStatus.CANCELLED, "Hủy");

		assertEquals(order.getTotalAmount(), order.getRefundAmount());
		assertNull(order.getRefundedAt());
		assertEquals(PaymentStatus.PAID, group.getPaymentStatus());
	}

	@Test
	void unpaidMomoCancellationDoesNotSetRefundAmount() {
		Order order = order(3L);
		order.setOrderGroup(group(PaymentMethod.MOMO, PaymentStatus.PENDING));
		when(orderItemRepository.findQuantitiesByOrderId(3L)).thenReturn(List.of());

		handler.apply(order, OrderStatus.NEW, OrderStatus.CANCELLED, "Hủy");

		assertNull(order.getRefundAmount());
		assertNull(order.getRefundedAt());
	}

	@Test
	void deliveredOrderUpdatesCountersAndCalculatesPlatformAndShopCommissionCorrectly() {
		Order order = order(4L);
		order.setStatus(OrderStatus.SHIPPING);
		order.setSubtotal(new BigDecimal("1000000"));
		order.setDiscountAmount(new BigDecimal("100000"));
		order.setPlatformDiscountAmount(new BigDecimal("40000"));
		order.setShippingFee(new BigDecimal("15000"));
		order.setOrderGroup(group(PaymentMethod.COD, PaymentStatus.PENDING));
		when(commissionService.getRate(31L)).thenReturn(new BigDecimal("5"));
		OrderItemQuantity quantity = quantity(12L, 2L);
		when(orderItemRepository.findQuantitiesByOrderId(4L)).thenReturn(List.of(quantity));

		handler.apply(order, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);

		assertEquals(NOW, order.getDeliveredAt());
		assertEquals(new BigDecimal("5"), order.getCommissionRate());
		assertEquals(new BigDecimal("47000"), order.getCommissionAmount());
		assertEquals(new BigDecimal("893000"), order.getShopRevenue());
		assertTrue(order.isCodCollected());
		verify(productRepository).increaseSoldCount(12L, 2);
	}

	@Test
	void commissionUsesWholeVndHalfUpRounding() {
		Order order = order(5L);
		order.setSubtotal(new BigDecimal("100010"));
		when(commissionService.getRate(31L)).thenReturn(new BigDecimal("5"));
		when(orderItemRepository.findQuantitiesByOrderId(5L)).thenReturn(List.of());

		handler.apply(order, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);

		assertEquals(new BigDecimal("5001"), order.getCommissionAmount());
		assertEquals(new BigDecimal("95009"), order.getShopRevenue());
	}

	@Test
	void platformDiscountIsExcludedFromShopCostAndShopDiscountReducesCommissionBase() {
		Order platformOnly = order(6L);
		platformOnly.setSubtotal(new BigDecimal("1000"));
		platformOnly.setDiscountAmount(new BigDecimal("100"));
		platformOnly.setPlatformDiscountAmount(new BigDecimal("100"));
		Order shopOnly = order(7L);
		shopOnly.setSubtotal(new BigDecimal("1000"));
		shopOnly.setDiscountAmount(new BigDecimal("100"));
		shopOnly.setPlatformDiscountAmount(null);
		Order noCoupon = order(8L);
		noCoupon.setSubtotal(new BigDecimal("1000"));
		noCoupon.setDiscountAmount(null);
		when(commissionService.getRate(31L)).thenReturn(new BigDecimal("5"));
		when(orderItemRepository.findQuantitiesByOrderId(6L)).thenReturn(List.of());
		when(orderItemRepository.findQuantitiesByOrderId(7L)).thenReturn(List.of());
		when(orderItemRepository.findQuantitiesByOrderId(8L)).thenReturn(List.of());

		handler.apply(platformOnly, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);
		handler.apply(shopOnly, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);
		handler.apply(noCoupon, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);

		assertEquals(new BigDecimal("50"), platformOnly.getCommissionAmount());
		assertEquals(new BigDecimal("45"), shopOnly.getCommissionAmount());
		assertEquals(new BigDecimal("50"), noCoupon.getCommissionAmount());
	}

	@Test
	void momoDeliveryDoesNotChangeCodCollectedFlag() {
		Order order = order(9L);
		order.setOrderGroup(group(PaymentMethod.MOMO, PaymentStatus.PAID));
		order.setCodCollected(true);
		when(commissionService.getRate(31L)).thenReturn(new BigDecimal("5"));
		when(orderItemRepository.findQuantitiesByOrderId(9L)).thenReturn(List.of());

		handler.apply(order, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null);

		assertTrue(order.isCodCollected());
	}

	@Test
	void nullOrNegativeCommissionRateFailsBeforeOrderOrProductMutation() {
		for (BigDecimal rate : new BigDecimal[] {null, new BigDecimal("-1")}) {
			Order order = order(10L);
			order.setStatus(OrderStatus.SHIPPING);
			when(commissionService.getRate(31L)).thenReturn(rate);

			assertThrows(IllegalStateException.class,
					() -> handler.apply(order, OrderStatus.SHIPPING, OrderStatus.DELIVERED, null));
			assertEquals(OrderStatus.SHIPPING, order.getStatus());
			assertNull(order.getDeliveredAt());
			assertNull(order.getCommissionAmount());
		}
		verifyNoInteractions(orderItemRepository, productRepository);
	}

	@Test
	void returnRequestAllowsThreeDaysAndExactLastMoment() {
		Order recent = order(11L);
		recent.setDeliveredAt(NOW.minusDays(3));
		Order exactlyAtDeadline = order(12L);
		exactlyAtDeadline.setDeliveredAt(NOW.minusDays(7));

		handler.validateBefore(recent, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED);
		handler.validateBefore(exactlyAtDeadline, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED);
	}

	@Test
	void returnRequestRejectsOneSecondPastDeadline() {
		Order order = order(13L);
		order.setDeliveredAt(NOW.minusDays(7).minusSeconds(1));

		BusinessException error = assertThrows(BusinessException.class,
				() -> handler.validateBefore(order, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED));

		assertEquals("Đơn hàng đã quá thời hạn đổi trả (7 ngày kể từ khi giao)", error.getMessage());
	}

	@Test
	void returnRequestUsesUpdatedAtWhenDeliveredAtIsMissing() {
		Order order = order(14L);
		order.setDeliveredAt(null);
		order.setUpdatedAt(NOW.minusDays(7));

		handler.validateBefore(order, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED);
	}

	@Test
	void refundReversesSoldCountAndPreservesCommissionSnapshot() {
		Order order = order(15L);
		order.setCommissionRate(new BigDecimal("5"));
		order.setCommissionAmount(new BigDecimal("47"));
		order.setShopRevenue(new BigDecimal("893"));
		OrderItemQuantity quantity = quantity(12L, 2L);
		when(orderItemRepository.findQuantitiesByOrderId(15L)).thenReturn(List.of(quantity));

		handler.apply(order, OrderStatus.RETURNED, OrderStatus.REFUNDED, "Đã hoàn tiền");

		verify(productRepository).decreaseSoldCount(12L, 2);
		verify(productRepository).increaseStock(12L, 2);
		assertEquals(order.getTotalAmount(), order.getRefundAmount());
		assertEquals(NOW, order.getRefundedAt());
		assertEquals(new BigDecimal("5"), order.getCommissionRate());
		assertEquals(new BigDecimal("47"), order.getCommissionAmount());
		assertEquals(new BigDecimal("893"), order.getShopRevenue());
	}

	@Test
	void unrelatedStatusChangesDoNotTouchProductOrCommissionRepositories() {
		for (OrderStatus status : List.of(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
				OrderStatus.DELIVERY_FAILED, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
				OrderStatus.RETURN_REJECTED, OrderStatus.RETURNED)) {
			handler.apply(order(20L), OrderStatus.NEW, status, null);
		}

		verifyNoInteractions(orderItemRepository, productRepository, commissionService);
	}

	private Order order(Long id) {
		Order order = new Order();
		order.setId(id);
		order.setOrderCode("ORD-" + id);
		order.setSubtotal(new BigDecimal("1000"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setTotalAmount(new BigDecimal("1000"));
		Shop shop = new Shop();
		shop.setId(31L);
		order.setShop(shop);
		return order;
	}

	private OrderGroup group(PaymentMethod method, PaymentStatus status) {
		OrderGroup group = new OrderGroup();
		group.setPaymentMethod(method);
		group.setPaymentStatus(status);
		return group;
	}

	private OrderItemQuantity quantity(Long productId, Long quantity) {
		OrderItemQuantity projection = org.mockito.Mockito.mock(OrderItemQuantity.class);
		when(projection.getProductId()).thenReturn(productId);
		when(projection.getQuantity()).thenReturn(quantity);
		return projection;
	}
}
