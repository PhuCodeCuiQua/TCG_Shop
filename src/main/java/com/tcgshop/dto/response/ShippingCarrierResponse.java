package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ShippingCarrierResponse(
		Long id,
		String name,
		String code,
		String logoUrl,
		BigDecimal baseFee,
		BigDecimal freeShipThreshold) {
}
