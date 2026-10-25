package com.tcgshop.service;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.dto.response.CancelOrderResultResponse;

public interface OrderCancellationService {
	CancelOrderResultResponse cancelMyOrder(Long orderId, CancelOrderRequest request);
}
