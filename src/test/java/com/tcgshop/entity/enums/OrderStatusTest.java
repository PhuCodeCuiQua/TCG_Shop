package com.tcgshop.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderStatusTest {
	@Test
	void revenueClassificationCoversEveryStatus() {
		Set<OrderStatus> revenueStatuses = Set.of(
				OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_REJECTED);

		for (OrderStatus status : OrderStatus.values()) {
			assertEquals(revenueStatuses.contains(status), status.countsAsRevenue(), status.name());
			assertEquals(revenueStatuses.contains(status), OrderStatus.REVENUE_STATUSES.contains(status), status.name());
		}
	}
}
