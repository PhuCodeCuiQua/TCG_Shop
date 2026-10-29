package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ShipperDashboardResponse(
		long ordersToDeliver,
		long deliveredToday,
		BigDecimal weeklyCodIncome,
		List<ShipperRecentOrderResponse> recentOrders) {
	public ShipperDashboardResponse {
		recentOrders = recentOrders == null ? List.of() : List.copyOf(recentOrders);
	}

	public ShipperDashboardResponse(long ordersToDeliver, long deliveredToday, BigDecimal weeklyCodIncome) {
		this(ordersToDeliver, deliveredToday, weeklyCodIncome, List.of());
	}
}
