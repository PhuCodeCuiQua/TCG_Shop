package com.tcgshop.service;

import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.response.PlaceOrderResponse;

public interface OrderPlacementService {
	PlaceOrderResponse placeOrder(PlaceOrderRequest request);

	PlaceOrderResponse getPlacedSummary(String groupCode);
}
