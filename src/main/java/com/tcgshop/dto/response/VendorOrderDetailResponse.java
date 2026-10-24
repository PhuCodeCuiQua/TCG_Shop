package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendorOrderDetailResponse(
		Long id,
		String orderCode,
		LocalDateTime createdAt,
		OrderStatus status,
		String statusLabel,
		String statusColor,
		String customerName,
		String recipientName,
		String recipientPhone,
		String shippingAddress,
		String paymentMethod,
		String paymentStatus,
		List<VendorOrderItemResponse> items,
		BigDecimal subtotal,
		BigDecimal discountAmount,
		BigDecimal shippingFee,
		BigDecimal totalAmount,
		List<VendorOrderTimelineResponse> timeline,
		boolean canRespond) {
	public VendorOrderDetailResponse {
		items = items == null ? List.of() : List.copyOf(items);
		timeline = timeline == null ? List.of() : List.copyOf(timeline);
	}
}
