package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViewedProductResponse(
		Long viewedId,
		Long productId,
		Long shopId,
		String shopName,
		String name,
		String seriesName,
		String gameName,
		BigDecimal price,
		String imageUrl,
		Integer soldCount,
		LocalDateTime viewedAt) {
}
