package com.tcgshop.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void revenueAndSoldCountStatusesAreSubsetsOfFinalizedCommissionStatuses() {
		assertTrue(OrderStatus.SOLD_COUNT_STATUSES.containsAll(OrderStatus.REVENUE_STATUSES));
		assertTrue(OrderStatus.COMMISSION_FINALIZED_STATUSES.containsAll(OrderStatus.SOLD_COUNT_STATUSES));

		Set<OrderStatus> excluded = Set.of(OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.PICKED_UP,
				OrderStatus.SHIPPING, OrderStatus.DELIVERY_FAILED, OrderStatus.CANCELLED);
		Set<OrderStatus> expectedSold = Set.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED,
				OrderStatus.RETURN_REJECTED, OrderStatus.RETURN_APPROVED, OrderStatus.RETURNED);
		Set<OrderStatus> expectedCommission = Set.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED,
				OrderStatus.RETURN_REJECTED, OrderStatus.RETURN_APPROVED, OrderStatus.RETURNED, OrderStatus.REFUNDED);
		for (OrderStatus status : OrderStatus.values()) {
			assertEquals(expectedSold.contains(status), OrderStatus.SOLD_COUNT_STATUSES.contains(status), status.name());
			assertEquals(expectedCommission.contains(status),
					OrderStatus.COMMISSION_FINALIZED_STATUSES.contains(status), status.name());
			if (excluded.contains(status)) {
				assertFalse(OrderStatus.REVENUE_STATUSES.contains(status), status.name());
				assertFalse(OrderStatus.SOLD_COUNT_STATUSES.contains(status), status.name());
				assertFalse(OrderStatus.COMMISSION_FINALIZED_STATUSES.contains(status), status.name());
			}
		}
		assertEquals(OrderStatus.values().length, excluded.size()
				+ OrderStatus.SOLD_COUNT_STATUSES.size()
				+ (OrderStatus.COMMISSION_FINALIZED_STATUSES.size() - OrderStatus.SOLD_COUNT_STATUSES.size()));
	}
}
