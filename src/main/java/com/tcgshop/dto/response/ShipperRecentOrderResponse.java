package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;

public record ShipperRecentOrderResponse(
		Long id,
		String orderCode,
		String customerName,
		OrderStatus status,
		String statusLabel,
		String statusColor,
		BigDecimal codAmount) { }
