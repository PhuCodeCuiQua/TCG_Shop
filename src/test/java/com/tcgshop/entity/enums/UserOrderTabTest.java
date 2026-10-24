package com.tcgshop.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserOrderTabTest {
	@Test
	void everyOrderStatusBelongsToExactlyOneNonAllTab() {
		Set<OrderStatus> seen = new HashSet<>();
		for (UserOrderTab tab : UserOrderTab.values()) {
			if (tab != UserOrderTab.ALL) {
				for (OrderStatus status : tab.getStatuses()) assertTrue(seen.add(status), status + " duplicated");
			}
		}
		assertEquals(EnumSet.allOf(OrderStatus.class), seen);
	}

	@Test
	void fromSlugDefaultsToAllAndIsCaseSensitive() {
		assertEquals(UserOrderTab.ALL, UserOrderTab.fromSlug(null));
		assertEquals(UserOrderTab.ALL, UserOrderTab.fromSlug(""));
		assertEquals(UserOrderTab.ALL, UserOrderTab.fromSlug("unknown"));
		assertEquals(UserOrderTab.ALL, UserOrderTab.fromSlug("NEW"));
		assertEquals(UserOrderTab.NEW, UserOrderTab.fromSlug("new"));
	}

	@Test
	void tabOfNeverReturnsAll() {
		for (OrderStatus status : OrderStatus.values()) {
			assertNotEquals(UserOrderTab.ALL, UserOrderTab.tabOf(status));
		}
	}
}
