package com.tcgshop.util;

import com.tcgshop.entity.enums.OrderStatus;

public final class OrderStatusUi {
	private OrderStatusUi() { }

	public static String color(OrderStatus status) {
		return switch (status) {
			case NEW, RETURN_REQUESTED -> "warning";
			case CONFIRMED, RETURN_APPROVED, RETURNED -> "info";
			case PICKED_UP, SHIPPING -> "primary";
			case DELIVERED, REFUNDED -> "success";
			case DELIVERY_FAILED, RETURN_REJECTED -> "danger";
			case CANCELLED -> "secondary";
		};
	}
}
