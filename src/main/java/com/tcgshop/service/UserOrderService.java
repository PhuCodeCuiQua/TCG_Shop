package com.tcgshop.service;

import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.UserOrderListResponse;

public interface UserOrderService {
	UserOrderListResponse listMyOrders(String tabSlug, String keyword, int page);

	OrderDetailResponse getMyOrder(Long id);
}
