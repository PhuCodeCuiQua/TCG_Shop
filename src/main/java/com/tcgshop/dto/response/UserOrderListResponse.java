package com.tcgshop.dto.response;

import java.util.List;

public record UserOrderListResponse(List<OrderTabResponse> tabs, PageResponse<OrderListItemResponse> orders,
		String selectedTab, String keyword) {
	public UserOrderListResponse {
		tabs = tabs == null ? List.of() : List.copyOf(tabs);
	}
}
