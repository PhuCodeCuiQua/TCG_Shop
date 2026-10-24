package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(Long orderId, String orderCode, String groupCode, OrderStatus status,
		String statusLabel, String statusColor, LocalDateTime createdAt, LocalDateTime deliveredAt,
		Long shopId, String shopName, List<OrderDetailItemResponse> items, BigDecimal subtotal,
		BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal totalAmount, String carrierName,
		String receiverName, String receiverPhone, String shippingAddress, String note, String cancelReason,
		String paymentMethodLabel, String paymentLabel, BigDecimal refundAmount, LocalDateTime refundedAt,
		String shipperName, List<StepResponse> steps, List<TimelineEntryResponse> timeline,
		List<SiblingOrderResponse> siblingOrders, boolean canCancel, boolean canRequestReturn) {
	public OrderDetailResponse {
		items = items == null ? List.of() : List.copyOf(items);
		steps = steps == null ? List.of() : List.copyOf(steps);
		timeline = timeline == null ? List.of() : List.copyOf(timeline);
		siblingOrders = siblingOrders == null ? List.of() : List.copyOf(siblingOrders);
	}
}
