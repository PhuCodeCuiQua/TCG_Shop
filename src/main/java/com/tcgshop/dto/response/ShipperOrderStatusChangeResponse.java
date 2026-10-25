package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;

public record ShipperOrderStatusChangeResponse(
		boolean success,
		Long orderId,
		OrderStatus status,
		String statusLabel,
		String statusColor,
		String message) {
}
