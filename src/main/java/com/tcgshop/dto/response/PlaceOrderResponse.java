package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderResponse(
		Long orderGroupId,
		String groupCode,
		PaymentMethod paymentMethod,
		BigDecimal grandTotal,
		List<PlacedOrderSummaryResponse> orders,
		String redirectUrl,
		boolean alreadyPlaced) {
	public PlaceOrderResponse {
		orders = orders == null ? List.of() : List.copyOf(orders);
	}
}
