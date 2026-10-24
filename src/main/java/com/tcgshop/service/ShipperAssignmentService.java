package com.tcgshop.service;

import com.tcgshop.dto.response.AssignableOrderResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperLoadResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ShipperAssignmentService {

	/**
	 * Danh sách đơn CONFIRMED.
	 *
	 * @param assigned false: chưa có shipper; true: đã có shipper nhưng chưa lấy hàng (có thể đổi)
	 */
	PageResponse<AssignableOrderResponse> searchOrders(boolean assigned, String keyword, Pageable pageable);

	/** Shipper đang hoạt động kèm số đơn đang giao. */
	List<ShipperLoadResponse> getShipperLoads();

	/** Phân công hoặc đổi shipper; gọi OrderService.assignShipper, thông báo do listener PA-40 gửi. */
	void assign(Long orderId, Long shipperId);
}