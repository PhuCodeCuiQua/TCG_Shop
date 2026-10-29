package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;

public record VendorOrderStatusResponse(
		Long orderId,
		OrderStatus status,
		String statusLabel,
		String statusColor) { }
