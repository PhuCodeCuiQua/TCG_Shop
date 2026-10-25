package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record VendorRevenueChartResponse(
		String groupBy,
		List<RevenuePoint> revenue,
		List<TopProduct> topProducts,
		List<OrderStatusCount> orderStatuses) {

	public record RevenuePoint(String label, BigDecimal amount) {
	}

	public record TopProduct(String productName, long quantitySold) {
	}

	public record OrderStatusCount(String status, String label, long orderCount) {
	}
}
