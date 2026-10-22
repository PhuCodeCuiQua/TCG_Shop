package com.tcgshop.service;

import com.tcgshop.entity.enums.OrderStatus;

public interface OrderService {
	/** Đổi trạng thái đơn con; người thao tác lấy từ CurrentUser. */
	void changeStatus(Long orderId, OrderStatus newStatus, String note);

	/** Đổi trạng thái do job hoặc hệ thống; actor luôn là SYSTEM. */
	void changeStatusBySystem(Long orderId, OrderStatus newStatus, String note);

	/** Manager/Admin phân công hoặc đổi shipper cho đơn đang CONFIRMED. */
	void assignShipper(Long orderId, Long shipperId);
}
