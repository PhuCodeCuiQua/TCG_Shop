package com.tcgshop.service.impl;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.entity.Order;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ReturnWindowPolicy {
	private final OrderProperties orderProperties;
	private final Clock clock;

	public ReturnWindowPolicy(OrderProperties orderProperties, Clock clock) {
		this.orderProperties = orderProperties;
		this.clock = clock;
	}

	public LocalDateTime deadline(Order order) {
		LocalDateTime base = order.getDeliveredAt() == null ? order.getUpdatedAt() : order.getDeliveredAt();
		return base == null ? null : base.plusDays(orderProperties.returnWindowDays());
	}

	public boolean isWithinWindow(Order order) {
		LocalDateTime deadline = deadline(order);
		return deadline != null && !LocalDateTime.now(clock).isAfter(deadline);
	}
}
