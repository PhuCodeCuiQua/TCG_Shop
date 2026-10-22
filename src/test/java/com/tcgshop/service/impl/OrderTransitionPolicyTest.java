package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.entity.enums.ActorType;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTransitionPolicyTest {
	private final OrderTransitionPolicy policy = new OrderTransitionPolicy();

	@ParameterizedTest
	@MethodSource("validTransitions")
	void acceptsEveryContractTransitionWithExactActorsAndNoteRequirement(
			OrderStatus from, OrderStatus to, Set<ActorType> actors, boolean noteRequired) {
		assertTrue(policy.isAllowed(from, to));
		assertEquals(actors, policy.allowedActors(from, to));
		assertEquals(noteRequired, policy.requiresNote(from, to));
	}

	@ParameterizedTest
	@MethodSource("invalidTransitions")
	void rejectsTransitionsMissingFromContract(OrderStatus from, OrderStatus to) {
		assertFalse(policy.isAllowed(from, to));
		assertEquals(Set.of(), policy.allowedActors(from, to));
		assertFalse(policy.requiresNote(from, to));
	}

	private static Stream<Arguments> validTransitions() {
		return Stream.of(
				Arguments.of(OrderStatus.NEW, OrderStatus.CONFIRMED, Set.of(ActorType.SHOP_OWNER), false),
				Arguments.of(OrderStatus.NEW, OrderStatus.CANCELLED,
						Set.of(ActorType.BUYER, ActorType.SHOP_OWNER, ActorType.MANAGER, ActorType.ADMIN, ActorType.SYSTEM), true),
				Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED,
						Set.of(ActorType.BUYER, ActorType.SHOP_OWNER, ActorType.MANAGER, ActorType.ADMIN), true),
				Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP,
						Set.of(ActorType.ASSIGNED_SHIPPER), false),
				Arguments.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
						Set.of(ActorType.ASSIGNED_SHIPPER), false),
				Arguments.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED,
						Set.of(ActorType.ASSIGNED_SHIPPER), false),
				Arguments.of(OrderStatus.SHIPPING, OrderStatus.DELIVERY_FAILED,
						Set.of(ActorType.ASSIGNED_SHIPPER), true),
				Arguments.of(OrderStatus.DELIVERY_FAILED, OrderStatus.SHIPPING,
						Set.of(ActorType.ASSIGNED_SHIPPER), false),
				Arguments.of(OrderStatus.DELIVERY_FAILED, OrderStatus.CANCELLED,
						Set.of(ActorType.MANAGER, ActorType.ADMIN), true),
				Arguments.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED,
						Set.of(ActorType.BUYER), true),
				Arguments.of(OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
						Set.of(ActorType.SHOP_OWNER), false),
				Arguments.of(OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_REJECTED,
						Set.of(ActorType.SHOP_OWNER), true),
				Arguments.of(OrderStatus.RETURN_REJECTED, OrderStatus.RETURN_APPROVED,
						Set.of(ActorType.MANAGER, ActorType.ADMIN), true),
				Arguments.of(OrderStatus.RETURN_APPROVED, OrderStatus.RETURNED,
						Set.of(ActorType.ASSIGNED_SHIPPER), false),
				Arguments.of(OrderStatus.RETURNED, OrderStatus.REFUNDED,
						Set.of(ActorType.MANAGER, ActorType.ADMIN), true));
	}

	private static Stream<Arguments> invalidTransitions() {
		return Stream.of(
				Arguments.of(OrderStatus.NEW, OrderStatus.DELIVERED),
				Arguments.of(OrderStatus.CANCELLED, OrderStatus.NEW),
				Arguments.of(OrderStatus.DELIVERED, OrderStatus.SHIPPING));
	}
}
