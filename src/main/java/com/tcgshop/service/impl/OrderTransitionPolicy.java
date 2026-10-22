package com.tcgshop.service.impl;

import com.tcgshop.entity.enums.ActorType;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bảng chính sách chuyển trạng thái đơn hàng theo hợp đồng chung.
 * Các cặp không khai báo trong bảng đều bị từ chối.
 */
@Component
public class OrderTransitionPolicy {
	private record TransitionRule(Set<ActorType> actors, boolean noteRequired) {
	}

	private static final Map<OrderStatus, Map<OrderStatus, TransitionRule>> TRANSITIONS = buildTransitions();

	public boolean isAllowed(OrderStatus from, OrderStatus to) {
		return findRule(from, to) != null;
	}

	public Set<ActorType> allowedActors(OrderStatus from, OrderStatus to) {
		TransitionRule rule = findRule(from, to);
		return rule == null ? Set.of() : rule.actors();
	}

	public boolean requiresNote(OrderStatus from, OrderStatus to) {
		TransitionRule rule = findRule(from, to);
		return rule != null && rule.noteRequired();
	}

	private TransitionRule findRule(OrderStatus from, OrderStatus to) {
		if (from == null || to == null) {
			return null;
		}
		return TRANSITIONS.getOrDefault(from, Map.of()).get(to);
	}

	private static Map<OrderStatus, Map<OrderStatus, TransitionRule>> buildTransitions() {
		Map<OrderStatus, Map<OrderStatus, TransitionRule>> transitions = new EnumMap<>(OrderStatus.class);
		add(transitions, OrderStatus.NEW, OrderStatus.CONFIRMED, Set.of(ActorType.SHOP_OWNER), false);
		add(transitions, OrderStatus.NEW, OrderStatus.CANCELLED,
				Set.of(ActorType.BUYER, ActorType.SHOP_OWNER, ActorType.MANAGER, ActorType.ADMIN, ActorType.SYSTEM), true);
		add(transitions, OrderStatus.CONFIRMED, OrderStatus.CANCELLED,
				Set.of(ActorType.BUYER, ActorType.SHOP_OWNER, ActorType.MANAGER, ActorType.ADMIN), true);
		add(transitions, OrderStatus.CONFIRMED, OrderStatus.PICKED_UP,
				Set.of(ActorType.ASSIGNED_SHIPPER), false);
		add(transitions, OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
				Set.of(ActorType.ASSIGNED_SHIPPER), false);
		add(transitions, OrderStatus.SHIPPING, OrderStatus.DELIVERED,
				Set.of(ActorType.ASSIGNED_SHIPPER), false);
		add(transitions, OrderStatus.SHIPPING, OrderStatus.DELIVERY_FAILED,
				Set.of(ActorType.ASSIGNED_SHIPPER), true);
		add(transitions, OrderStatus.DELIVERY_FAILED, OrderStatus.SHIPPING,
				Set.of(ActorType.ASSIGNED_SHIPPER), false);
		add(transitions, OrderStatus.DELIVERY_FAILED, OrderStatus.CANCELLED,
				Set.of(ActorType.MANAGER, ActorType.ADMIN), true);
		add(transitions, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED,
				Set.of(ActorType.BUYER), true);
		add(transitions, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
				Set.of(ActorType.SHOP_OWNER), false);
		add(transitions, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_REJECTED,
				Set.of(ActorType.SHOP_OWNER), true);
		add(transitions, OrderStatus.RETURN_REJECTED, OrderStatus.RETURN_APPROVED,
				Set.of(ActorType.MANAGER, ActorType.ADMIN), true);
		add(transitions, OrderStatus.RETURN_APPROVED, OrderStatus.RETURNED,
				Set.of(ActorType.ASSIGNED_SHIPPER), false);
		add(transitions, OrderStatus.RETURNED, OrderStatus.REFUNDED,
				Set.of(ActorType.MANAGER, ActorType.ADMIN), true);

		Map<OrderStatus, Map<OrderStatus, TransitionRule>> immutable = new EnumMap<>(OrderStatus.class);
		transitions.forEach((status, rules) -> immutable.put(status, Map.copyOf(rules)));
		return Map.copyOf(immutable);
	}

	private static void add(Map<OrderStatus, Map<OrderStatus, TransitionRule>> transitions,
			OrderStatus from, OrderStatus to, Set<ActorType> actors, boolean noteRequired) {
		Map<OrderStatus, TransitionRule> rules = transitions.computeIfAbsent(from,
				ignored -> new EnumMap<>(OrderStatus.class));
		rules.put(to, new TransitionRule(Set.copyOf(actors), noteRequired));
	}
}
