package com.tcgshop.service;

import com.tcgshop.dto.response.ManagerOrderDetailResponse;
import com.tcgshop.dto.response.ManagerOrderSummaryResponse;
import com.tcgshop.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManagerOrderService {
	Page<ManagerOrderSummaryResponse> searchOrders(String keyword, OrderStatus status, Pageable pageable);
	ManagerOrderDetailResponse getOrderDetail(Long orderId);
}
