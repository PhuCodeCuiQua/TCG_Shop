package com.tcgshop.dto.response;

import java.util.List;

public record ShipperOrderListResponse(
		List<ShipperOrderTabResponse> tabs,
		PageResponse<ShipperOrderSummaryResponse> orders,
		String selectedTab) {
	public ShipperOrderListResponse {
		tabs = tabs == null ? List.of() : List.copyOf(tabs);
	}
}
