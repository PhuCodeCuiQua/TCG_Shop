package com.tcgshop.entity.enums;

import java.util.Set;

public enum UserOrderTab {
	ALL("all", "Tất cả", Set.of()),
	NEW("new", "Chờ xác nhận", Set.of(OrderStatus.NEW)),
	CONFIRMED("confirmed", "Đã xác nhận", Set.of(OrderStatus.CONFIRMED)),
	SHIPPING("shipping", "Đang giao", Set.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
			OrderStatus.DELIVERY_FAILED)),
	DELIVERED("delivered", "Đã giao", Set.of(OrderStatus.DELIVERED)),
	CANCELLED("cancelled", "Đã hủy", Set.of(OrderStatus.CANCELLED)),
	RETURN("return", "Trả hàng/Hoàn tiền", Set.of(OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
			OrderStatus.RETURN_REJECTED, OrderStatus.RETURNED, OrderStatus.REFUNDED));

	private final String slug;
	private final String label;
	private final Set<OrderStatus> statuses;

	UserOrderTab(String slug, String label, Set<OrderStatus> statuses) {
		this.slug = slug;
		this.label = label;
		this.statuses = statuses;
	}

	public String getSlug() { return slug; }
	public String getLabel() { return label; }
	public Set<OrderStatus> getStatuses() { return statuses; }

	public static UserOrderTab fromSlug(String slug) {
		if (slug != null) {
			for (UserOrderTab tab : values()) {
				if (tab.slug.equals(slug)) return tab;
			}
		}
		return ALL;
	}

	public static UserOrderTab tabOf(OrderStatus status) {
		for (UserOrderTab tab : values()) {
			if (tab != ALL && tab.statuses.contains(status)) return tab;
		}
		throw new IllegalArgumentException("Order status is not assigned to a user tab: " + status);
	}
}
