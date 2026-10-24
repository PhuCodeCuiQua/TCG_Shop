package com.tcgshop.event;

import com.tcgshop.entity.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record OrderGroupPlacedEvent(
		Long orderGroupId,
		String groupCode,
		Long buyerId,
		String buyerEmail,
		String buyerName,
		PaymentMethod paymentMethod,
		BigDecimal grandTotal,
		List<String> orderCodes) {
	public OrderGroupPlacedEvent {
		orderCodes = orderCodes == null ? List.of() : List.copyOf(orderCodes);
	}
}
