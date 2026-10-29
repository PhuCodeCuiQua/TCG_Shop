package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShipperOrderDetailResponse(
		Long id,
		String orderCode,
		LocalDateTime createdAt,
		OrderStatus status,
		String statusLabel,
		String statusColor,
		String shopName,
		String pickupAddress,
		String shopPhone,
		String pickupNote,
		String recipientName,
		String recipientPhone,
		String shippingAddress,
		BigDecimal codAmount,
		BigDecimal totalAmount,
		List<ShipperOrderItemResponse> items) {
	public ShipperOrderDetailResponse {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
