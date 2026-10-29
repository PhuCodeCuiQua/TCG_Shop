package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tcgshop.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderStatusUiTest {
	@Test
	void everyOrderStatusHasBootstrapColor() {
		for (OrderStatus status : OrderStatus.values()) assertNotNull(OrderStatusUi.color(status), status.name());
	}
}
