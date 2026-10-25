package com.tcgshop.dto.response;

import java.util.List;

public record ShipperDeliveryChartResponse(List<String> labels, List<Long> orderCounts) {
	public ShipperDeliveryChartResponse {
		labels = labels == null ? List.of() : List.copyOf(labels);
		orderCounts = orderCounts == null ? List.of() : List.copyOf(orderCounts);
	}
}
