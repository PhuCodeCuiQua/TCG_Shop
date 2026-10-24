package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record PlacedOrderSummaryResponse(
		Long orderId,
		String orderCode,
		Long shopId,
		String shopName,
		BigDecimal totalAmount) {
}
