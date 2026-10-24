package com.tcgshop.service.impl;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.repository.OrderItemQuantity;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.service.CommissionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusSideEffectHandler {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final CommissionService commissionService;
	private final OrderProperties orderProperties;
	private final Clock clock;

	public OrderStatusSideEffectHandler(OrderItemRepository orderItemRepository,
			ProductRepository productRepository, CommissionService commissionService,
			OrderProperties orderProperties, Clock clock) {
		this.orderItemRepository = orderItemRepository;
		this.productRepository = productRepository;
		this.commissionService = commissionService;
		this.orderProperties = orderProperties;
		this.clock = clock;
	}

	public void validateBefore(Order order, OrderStatus from, OrderStatus to) {
		if (to != OrderStatus.RETURN_REQUESTED) {
			return;
		}

		LocalDateTime deliveredAt = order.getDeliveredAt() == null ? order.getUpdatedAt() : order.getDeliveredAt();
		int returnWindowDays = orderProperties.returnWindowDays();
		LocalDateTime now = LocalDateTime.now(clock);
		if (deliveredAt == null || now.isAfter(deliveredAt.plusDays(returnWindowDays))) {
			throw new BusinessException("Đơn hàng đã quá thời hạn đổi trả ("
					+ returnWindowDays + " ngày kể từ khi giao)");
		}
	}

	public void apply(Order order, OrderStatus from, OrderStatus to, String note) {
		switch (to) {
			case CANCELLED -> applyCancellation(order, note);
			case DELIVERED -> applyDelivery(order);
			case REFUNDED -> applyRefund(order);
			default -> {
				// Các trạng thái còn lại không làm thay đổi tồn kho, lượt bán hay số liệu doanh thu.
			}
		}
	}

	private void applyCancellation(Order order, String note) {
		// Khi hủy, khôi phục từng mặt hàng theo thứ tự productId mà repository đã sắp xếp.
		order.setCancelReason(note == null ? null : note.trim());
		for (OrderItemQuantity quantity : quantities(order)) {
			productRepository.increaseStock(quantity.getProductId(), Math.toIntExact(quantity.getQuantity()));
		}

		OrderGroup group = order.getOrderGroup();
		if (group != null && group.getPaymentMethod() == PaymentMethod.MOMO
				&& group.getPaymentStatus() == PaymentStatus.PAID) {
			order.setRefundAmount(order.getTotalAmount());
			order.setRefundedAt(null);
			// TODO(PU-17): Cập nhật trạng thái thanh toán cấp nhóm khi tích hợp quy trình hoàn tiền MoMo.
		}
		// Chính sách đã chốt: hủy đơn không hoàn lại lượt sử dụng mã giảm giá.
	}

	private void applyDelivery(Order order) {
		// Tính và kiểm tra rate trước mọi thay đổi để rate lỗi không để lại dữ liệu dở dang ngoài transaction.
		BigDecimal rate = commissionService.getRate(order.getShop().getId());
		if (rate == null || rate.signum() < 0) {
			throw new IllegalStateException("Tỷ lệ hoa hồng không được null hoặc âm");
		}

		BigDecimal platformDiscount = valueOrZero(order.getPlatformDiscountAmount());
		BigDecimal shopDiscount = valueOrZero(order.getDiscountAmount()).subtract(platformDiscount).max(ZERO);
		BigDecimal base = valueOrZero(order.getSubtotal()).subtract(shopDiscount).max(ZERO);
		// Cơ sở tính chỉ trừ phần giảm do shop chịu; mã sàn do nền tảng chịu và phí ship không tính hoa hồng.
		BigDecimal commission = base.multiply(rate).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
		BigDecimal shopRevenue = base.subtract(commission);

		for (OrderItemQuantity quantity : quantities(order)) {
			productRepository.increaseSoldCount(quantity.getProductId(), Math.toIntExact(quantity.getQuantity()));
		}
		order.setDeliveredAt(LocalDateTime.now(clock));
		order.setCommissionRate(rate);
		order.setCommissionAmount(commission);
		order.setShopRevenue(shopRevenue);
		if (order.getOrderGroup() != null && order.getOrderGroup().getPaymentMethod() == PaymentMethod.COD) {
			order.setCodCollected(true);
		}
	}

	private void applyRefund(Order order) {
		for (OrderItemQuantity quantity : quantities(order)) {
			productRepository.decreaseSoldCount(quantity.getProductId(), Math.toIntExact(quantity.getQuantity()));
			productRepository.increaseStock(quantity.getProductId(), Math.toIntExact(quantity.getQuantity()));
		}
		order.setRefundAmount(order.getTotalAmount());
		order.setRefundedAt(LocalDateTime.now(clock));
		// Hoa hồng và shop_revenue đã chốt được giữ nguyên; REFUNDED không thuộc nhóm trạng thái doanh thu.
	}

	private List<OrderItemQuantity> quantities(Order order) {
		return orderItemRepository.findQuantitiesByOrderId(order.getId());
	}

	private BigDecimal valueOrZero(BigDecimal amount) {
		return amount == null ? ZERO : amount;
	}
}
