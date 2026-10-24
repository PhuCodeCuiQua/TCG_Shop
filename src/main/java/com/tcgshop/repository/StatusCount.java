package com.tcgshop.repository;

import com.tcgshop.entity.enums.OrderStatus;

public interface StatusCount {
	OrderStatus getStatus();
	Long getTotal();
}
