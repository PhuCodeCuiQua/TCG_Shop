package com.tcgshop.service;

import com.tcgshop.dto.request.ShipperOrderStatusChangeRequest;
import com.tcgshop.dto.response.ShipperOrderStatusChangeResponse;

public interface ShipperOrderStatusService {
	ShipperOrderStatusChangeResponse changeStatus(Long orderId, ShipperOrderStatusChangeRequest request);
}
