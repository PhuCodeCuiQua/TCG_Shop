package com.tcgshop.event;

import java.util.List;

/** Sự kiện báo Manager/Admin sau khi đơn đã được xác nhận và chờ phân công shipper. */
public record OrderConfirmedEvent(Long orderId, String orderCode, List<Long> recipientIds) {

	public OrderConfirmedEvent {
		recipientIds = List.copyOf(recipientIds);
	}
}
