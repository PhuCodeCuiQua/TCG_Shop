package com.tcgshop.event;

/** Sự kiện phát sau khi phân công hoặc đổi shipper cho một đơn hàng. */
public record OrderAssignedEvent(
		Long orderId,
		String orderCode,
		Long shipperId,
		Long buyerId,
		Long assignedById) {
}
