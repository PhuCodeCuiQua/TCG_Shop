package com.tcgshop.repository;

import com.tcgshop.entity.enums.OrderStatus;

public interface ShipperOrderStatusCount {
	OrderStatus getStatus();

	Long getTotal();
}
