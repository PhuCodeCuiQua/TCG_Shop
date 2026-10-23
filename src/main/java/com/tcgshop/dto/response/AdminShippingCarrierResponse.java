package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record AdminShippingCarrierResponse(
		Long id,
		String name,
		String code,
		String logoUrl,
		BigDecimal baseFee,
		BigDecimal freeShipThreshold,
		boolean active,
		long usedOrderCount) {
}
