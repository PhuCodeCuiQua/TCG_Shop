package com.tcgshop.service;

import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderListResponse;
import org.springframework.data.domain.Pageable;

public interface ShipperOrderService {
	ShipperOrderListResponse getOrders(String tab, Pageable pageable);

	ShipperOrderDetailResponse getOrderDetail(Long orderId);
}
