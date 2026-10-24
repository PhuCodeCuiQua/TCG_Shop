package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.OrderListItemResponse;
import com.tcgshop.dto.response.StepState;
import com.tcgshop.dto.response.UserOrderListResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.UserOrderTab;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderItemRow;
import com.tcgshop.repository.OrderItemRow;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.StatusCount;
import com.tcgshop.repository.TimelineRow;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceImplTest {
	private static final Long BUYER_ID = 7L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

	@Mock private OrderRepository orderRepository;
	@Mock private OrderItemRepository orderItemRepository;
	@Mock private OrderStatusHistoryRepository historyRepository;
	@Mock private CurrentUser currentUser;
	@Mock private OrderProperties orderProperties;

	@InjectMocks private UserOrderServiceImpl service;
	private Clock clock;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
		service = new UserOrderServiceImpl(orderRepository, orderItemRepository, historyRepository,
				currentUser, orderProperties, clock);
		when(currentUser.getId()).thenReturn(BUYER_ID);
	}

	@Test
	void listUsesGroupedCountsAndNormalizesPageAndKeywordWhileLimitingPreviewItems() {
		List<StatusCount> counts = List.of(statusCount(OrderStatus.NEW, 2), statusCount(OrderStatus.CONFIRMED, 1),
				statusCount(OrderStatus.PICKED_UP, 1), statusCount(OrderStatus.SHIPPING, 2),
				statusCount(OrderStatus.CANCELLED, 1));
		when(orderRepository.countGroupedByStatus(BUYER_ID)).thenReturn(counts);
		Order order = order(101L, OrderStatus.NEW);
		when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(order), Pageable.ofSize(10), 1));
		List<OrderItemRow> previewRows = itemRows(5, 101L);
		when(orderItemRepository.findRowsByOrderIdIn(List.of(101L))).thenReturn(previewRows);

		String longKeyword = "  " + "x".repeat(55) + "   multiple   spaces  ";
		UserOrderListResponse response = service.listMyOrders("new", longKeyword, -4);

		assertEquals("x".repeat(50), response.keyword());
		assertEquals("new", response.selectedTab());
		assertEquals(7, response.tabs().stream().filter(tab -> tab.slug().equals("all")).findFirst().orElseThrow().count());
		assertEquals(2, response.tabs().stream().filter(tab -> tab.slug().equals("new")).findFirst().orElseThrow().count());
		assertTrue(response.tabs().stream().filter(tab -> tab.slug().equals("new")).findFirst().orElseThrow().active());
		assertFalse(response.tabs().stream().filter(tab -> tab.slug().equals("all")).findFirst().orElseThrow().active());
		OrderListItemResponse item = response.orders().content().get(0);
		assertEquals(5, item.itemCount());
		assertEquals(3, item.previewItems().size());
		assertEquals(2, item.moreItemsCount());
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(orderRepository).findAll(any(Specification.class), pageable.capture());
		assertEquals(0, pageable.getValue().getPageNumber());
		assertEquals(10, pageable.getValue().getPageSize());
		assertEquals(List.of("createdAt: DESC", "id: DESC"), pageable.getValue().getSort().stream()
				.map(sortOrder -> sortOrder.getProperty() + ": " + sortOrder.getDirection()).toList());
	}

	@Test
	void unknownTabSelectsAllAndBlankKeywordBecomesNull() {
		when(orderRepository.countGroupedByStatus(BUYER_ID)).thenReturn(List.of());
		when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		UserOrderListResponse response = service.listMyOrders("DOES_NOT_EXIST", "   ", 0);
		assertEquals("all", response.selectedTab());
		assertEquals(null, response.keyword());
		assertTrue(response.tabs().get(0).active());
		assertEquals(0, response.tabs().get(0).count());
	}

	@Test
	void getMyOrderReturnsNotFoundWhenNoOwnedOrderMatches() {
		when(orderRepository.findDetailByIdAndUserId(12L, BUYER_ID)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.getMyOrder(12L));
	}

	@Test
	void detailMapsTimelineActorsHidesAssignmentNoteAndIncludesSiblingOrders() {
		Order order = order(201L, OrderStatus.DELIVERED);
		order.getOrderGroup().setPaymentMethod(PaymentMethod.COD);
		order.setCodCollected(true);
		order.setDeliveredAt(NOW.minusDays(2));
		User buyer = user(BUYER_ID, "buyer");
		User owner = user(8L, "owner");
		User shipper = user(9L, "shipper");
		order.setUser(buyer);
		order.getShop().setOwner(owner);
		order.setShipper(shipper);
		when(orderProperties.returnWindowDays()).thenReturn(7);
		when(orderRepository.findDetailByIdAndUserId(201L, BUYER_ID)).thenReturn(Optional.of(order));
		List<OrderItemRow> detailRows = List.of(detailRow(201L));
		when(orderItemRepository.findRowsByOrderIdIn(List.of(201L))).thenReturn(detailRows);
		TimelineRow assignment = timeline(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED, 8L,
				"Phân công shipper: Tên không được lộ", NOW.minusDays(3));
		List<TimelineRow> timelineRows = List.of(
				timeline(null, OrderStatus.NEW, null, "", NOW.minusDays(5)),
				timeline(OrderStatus.NEW, OrderStatus.CONFIRMED, 8L, "", NOW.minusDays(4)),
				assignment,
				timeline(OrderStatus.SHIPPING, OrderStatus.DELIVERED, 9L, "  delivered  ", NOW.minusDays(2)));
		when(historyRepository.findTimelineByOrderId(201L)).thenReturn(timelineRows);
		Order sibling = order(202L, OrderStatus.SHIPPING);
		when(orderRepository.findByOrderGroupIdAndIdNotAndUserIdOrderByIdAsc(20L, 201L, BUYER_ID))
				.thenReturn(List.of(sibling));

		OrderDetailResponse response = service.getMyOrder(201L);

		assertEquals("Đã thanh toán", response.paymentLabel());
		assertEquals(5, response.steps().size());
		assertTrue(response.steps().stream().allMatch(step -> step.state() == StepState.DONE));
		assertEquals(List.of("Hệ thống", "Cửa hàng", "Cửa hàng", "Người giao hàng"),
				response.timeline().stream().map(entry -> entry.actorLabel()).toList());
		assertEquals("Đã phân công người giao hàng", response.timeline().get(2).statusLabel());
		assertEquals(null, response.timeline().get(2).note());
		assertEquals("delivered", response.timeline().get(3).note());
		assertEquals(1, response.siblingOrders().size());
		assertTrue(response.canRequestReturn());
		assertFalse(response.canCancel());
		verify(orderItemRepository).findRowsByOrderIdIn(List.of(201L));
}

	@Test
	void paymentLabelsCoverCodMomoAndRefundBranches() {
		for (PaymentStatus status : PaymentStatus.values()) {
			Order order = order(300L + status.ordinal(), OrderStatus.CANCELLED);
			order.getOrderGroup().setPaymentMethod(PaymentMethod.MOMO);
			order.getOrderGroup().setPaymentStatus(status);
			OrderDetailResponse response = detail(order);
			assertEquals(switch (status) {
				case PENDING -> "Chờ thanh toán";
				case PAID -> "Đã thanh toán";
				case FAILED -> "Thanh toán thất bại";
				case REFUND_PENDING -> "Chờ hoàn tiền";
				case REFUNDED -> "Đã hoàn tiền";
			}, response.paymentLabel());
		}
		Order pendingRefund = order(310L, OrderStatus.CANCELLED);
		pendingRefund.setRefundAmount(new BigDecimal("500"));
		assertEquals("Chờ hoàn tiền", detail(pendingRefund).paymentLabel());
		pendingRefund.setRefundedAt(NOW);
		assertEquals("Đã hoàn tiền", detail(pendingRefund).paymentLabel());
		Order cod = order(311L, OrderStatus.NEW);
		assertEquals("Chưa thanh toán (thanh toán khi nhận hàng)", detail(cod).paymentLabel());
		cod.setCodCollected(true);
		assertEquals("Đã thanh toán", detail(cod).paymentLabel());
	}

	@Test
	void stepsAndCancellationActionsFollowEachStatus() {
		for (int index = 0; index < 5; index++) {
			OrderStatus status = List.of(OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.PICKED_UP,
					OrderStatus.SHIPPING, OrderStatus.DELIVERED).get(index);
			Order order = order(400L + index, status);
			OrderDetailResponse response = detail(order);
			assertEquals(5, response.steps().size());
			for (int stepIndex = 0; stepIndex < 5; stepIndex++) {
				StepState expected = index == 4 || stepIndex < index ? StepState.DONE
						: stepIndex == index ? StepState.CURRENT : StepState.PENDING;
				assertEquals(expected, response.steps().get(stepIndex).state());
			}
			assertEquals(status == OrderStatus.NEW || status == OrderStatus.CONFIRMED, response.canCancel());
		}
		for (OrderStatus status : List.of(OrderStatus.CANCELLED, OrderStatus.DELIVERY_FAILED,
				OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED, OrderStatus.RETURNED)) {
			OrderDetailResponse response = detail(order(500L + status.ordinal(), status));
			assertTrue(response.steps().isEmpty());
			assertFalse(response.canCancel());
		}
	}

	@Test
	void returnWindowUsesDeliveredAtInclusiveDeadlineAndFallsBackToUpdatedAt() {
		when(orderProperties.returnWindowDays()).thenReturn(7);
		Order order = order(601L, OrderStatus.DELIVERED);
		order.setDeliveredAt(NOW.minusDays(7));
		assertTrue(detail(order).canRequestReturn());
		order.setDeliveredAt(NOW.minusDays(7).minusSeconds(1));
		assertFalse(detail(order).canRequestReturn());
		order.setDeliveredAt(null);
		order.setUpdatedAt(NOW.minusDays(7));
		assertTrue(detail(order).canRequestReturn());
		order.setStatus(OrderStatus.SHIPPING);
		assertFalse(detail(order).canRequestReturn());
	}

	private OrderDetailResponse detail(Order order) {
		when(orderRepository.findDetailByIdAndUserId(order.getId(), BUYER_ID)).thenReturn(Optional.of(order));
		when(orderItemRepository.findRowsByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());
		when(historyRepository.findTimelineByOrderId(order.getId())).thenReturn(List.of());
		when(orderRepository.findByOrderGroupIdAndIdNotAndUserIdOrderByIdAsc(
				order.getOrderGroup().getId(), order.getId(), BUYER_ID)).thenReturn(List.of());
		return service.getMyOrder(order.getId());
	}

	private Order order(Long id, OrderStatus status) {
		Order order = new Order();
		order.setId(id);
		order.setOrderCode("it-order-" + id);
		order.setStatus(status);
		order.setCreatedAt(NOW);
		order.setSubtotal(new BigDecimal("1000"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setShippingFee(BigDecimal.ZERO);
		order.setTotalAmount(new BigDecimal("1000"));
		OrderGroup group = new OrderGroup();
		group.setId(20L);
		group.setGroupCode("it-group-20");
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		order.setOrderGroup(group);
		Shop shop = new Shop();
		shop.setId(30L);
		shop.setName("it-shop");
		shop.setOwner(user(8L, "owner"));
		order.setShop(shop);
		return order;
	}

	private User user(Long id, String name) {
		User user = new User();
		user.setId(id);
		user.setFullName(name);
		return user;
	}

	private StatusCount statusCount(OrderStatus status, long count) {
		StatusCount result = mock(StatusCount.class);
		when(result.getStatus()).thenReturn(status);
		when(result.getTotal()).thenReturn(count);
		return result;
	}

	private List<OrderItemRow> itemRows(int count, Long orderId) {
		List<OrderItemRow> rows = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			rows.add(new TestOrderItemRow(orderId, (long) index, (long) index + 1,
					"it-product-" + index, null, null, index + 1, new BigDecimal("100"),
					new BigDecimal("100")));
		}
		return rows;
	}

	private OrderItemRow detailRow(Long orderId) {
		return new TestOrderItemRow(orderId, 1L, 33L, "it-card", "it-image", CardCondition.NEAR_MINT,
				2, new BigDecimal("500"), new BigDecimal("1000"));
	}

	private TimelineRow timeline(OrderStatus previous, OrderStatus next, Long actorId, String note,
			LocalDateTime createdAt) {
		return new TestTimelineRow(previous, next, note, createdAt, actorId);
	}

	private record TestTimelineRow(OrderStatus previousStatus, OrderStatus newStatus, String note,
			LocalDateTime createdAt, Long actorId) implements TimelineRow {
		@Override public OrderStatus getPreviousStatus() { return previousStatus; }
		@Override public OrderStatus getNewStatus() { return newStatus; }
		@Override public String getNote() { return note; }
		@Override public LocalDateTime getCreatedAt() { return createdAt; }
		@Override public Long getActorId() { return actorId; }
	}

	private record TestOrderItemRow(Long orderId, Long itemId, Long productId, String productName,
			String productImage, CardCondition cardCondition, Integer quantity, BigDecimal price,
			BigDecimal totalPrice) implements OrderItemRow {
		@Override public Long getOrderId() { return orderId; }
		@Override public Long getItemId() { return itemId; }
		@Override public Long getProductId() { return productId; }
		@Override public String getProductName() { return productName; }
		@Override public String getProductImage() { return productImage; }
		@Override public CardCondition getCardCondition() { return cardCondition; }
		@Override public Integer getQuantity() { return quantity; }
		@Override public BigDecimal getPrice() { return price; }
		@Override public BigDecimal getTotalPrice() { return totalPrice; }
	}
}
