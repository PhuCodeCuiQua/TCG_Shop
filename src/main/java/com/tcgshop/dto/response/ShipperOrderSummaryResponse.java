package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipperOrderSummaryResponse(
		Long id,
		String orderCode,
		LocalDateTime createdAt,
		String shippingAddress,
		BigDecimal codAmount,
		OrderStatus status,
		String statusLabel,
		String statusColor) { }
