package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.entity.Order;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReturnWindowPolicyTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 10, 12, 0);
	private final ReturnWindowPolicy policy = new ReturnWindowPolicy(new OrderProperties(7, 3),
			Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC));

	@Test
	void acceptsBeforeAndExactlyAtDeadlineButRejectsOneSecondLate() {
		Order order = new Order();
		order.setDeliveredAt(NOW.minusDays(7).plusSeconds(1));
		assertTrue(policy.isWithinWindow(order));
		order.setDeliveredAt(NOW.minusDays(7));
		assertTrue(policy.isWithinWindow(order));
		assertEquals(NOW, policy.deadline(order));
		order.setDeliveredAt(NOW.minusDays(7).minusSeconds(1));
		assertFalse(policy.isWithinWindow(order));
	}

	@Test
	void fallsBackToUpdatedAtWhenDeliveryTimestampIsMissing() {
		Order order = new Order();
		order.setUpdatedAt(NOW.minusDays(7));
		assertTrue(policy.isWithinWindow(order));
		assertEquals(NOW, policy.deadline(order));
	}
}
