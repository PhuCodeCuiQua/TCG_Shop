package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorRevenueEntryResponse(
		Long orderId,
		String orderCode,
		LocalDateTime deliveredAt,
		BigDecimal revenueBase,
		BigDecimal commissionAmount,
		BigDecimal shopRevenue) {
}
