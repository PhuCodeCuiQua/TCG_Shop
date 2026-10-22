package com.tcgshop.event;

import com.tcgshop.entity.enums.OrderStatus;
import java.time.LocalDateTime;

/** Sự kiện phát sau mỗi lần đổi trạng thái của đơn hàng. */
public record OrderStatusChangedEvent(
		Long orderId,
		String orderCode,
		Long buyerId,
		Long shopId,
		Long shopOwnerId,
		Long shipperId,
		OrderStatus oldStatus,
		OrderStatus newStatus,
		Long actorId,
		String note,
		LocalDateTime occurredAt) {
}
