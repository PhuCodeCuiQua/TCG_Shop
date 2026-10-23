package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ShopCommissionResponse(
		Long id,
		String name,
		String slug,
		BigDecimal overrideRate,
		BigDecimal effectiveRate,
		boolean inheritingDefault) {
}
