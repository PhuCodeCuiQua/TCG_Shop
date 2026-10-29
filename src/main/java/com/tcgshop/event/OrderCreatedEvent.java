package com.tcgshop.event;

/** Sự kiện phát một lần cho mỗi đơn con sau khi đặt hàng thành công. */
public record OrderCreatedEvent(
		Long orderId,
		String orderCode,
		Long orderGroupId,
		Long buyerId,
		Long shopId,
		Long shopOwnerId) {
}
