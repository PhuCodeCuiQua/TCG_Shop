package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
		Long itemId,
		Long productId,
		String productName,
		String imageUrl,
		String cardCondition,
		BigDecimal unitPrice,
		BigDecimal originalPrice,
		int quantity,
		Integer stock,
		BigDecimal lineTotal,
		boolean selected,
		boolean available,
		String unavailableReason,
		boolean quantityExceedsStock,
		boolean priceChanged,
		BigDecimal previousPrice) { }
