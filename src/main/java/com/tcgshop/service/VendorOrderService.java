package com.tcgshop.service;

import com.tcgshop.dto.request.VendorOrderStatusChangeRequest;
import com.tcgshop.dto.response.VendorOrderDetailResponse;
import com.tcgshop.dto.response.VendorOrderListResponse;
import com.tcgshop.dto.response.VendorOrderStatusResponse;
import org.springframework.data.domain.Pageable;

public interface VendorOrderService {
	VendorOrderListResponse getOrders(String tab, String keyword, Pageable pageable);

	VendorOrderDetailResponse getOrderDetail(Long orderId);

	VendorOrderStatusResponse changeOrderStatus(Long orderId, VendorOrderStatusChangeRequest request);
}
