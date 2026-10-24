package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record VendorRevenueSummaryResponse(
		BigDecimal revenueBase,
		BigDecimal commissionTotal,
		BigDecimal receivedTotal,
		long completedOrders,
		BigDecimal averageOrderValue) {
}
