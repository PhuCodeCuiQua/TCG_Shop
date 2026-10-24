package com.tcgshop.repository;

import com.tcgshop.entity.enums.OrderStatus;

public interface VendorOrderStatusCount {
	OrderStatus getStatus();
	Long getTotal();
}
