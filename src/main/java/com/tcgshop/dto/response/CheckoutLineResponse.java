package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record CheckoutLineResponse(
		Long productId,
		String productName,
		String imageUrl,
		String cardCondition,
		BigDecimal unitPrice,
		BigDecimal originalPrice,
		int quantity,
		int stock,
		BigDecimal lineTotal,
		String problem) {
}
