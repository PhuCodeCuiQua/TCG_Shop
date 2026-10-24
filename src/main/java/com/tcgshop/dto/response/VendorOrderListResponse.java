package com.tcgshop.dto.response;

import java.util.List;

public record VendorOrderListResponse(
		List<VendorOrderTabResponse> tabs,
		PageResponse<VendorOrderSummaryResponse> orders,
		String selectedTab,
		String keyword) {
	public VendorOrderListResponse {
		tabs = tabs == null ? List.of() : List.copyOf(tabs);
	}
}
