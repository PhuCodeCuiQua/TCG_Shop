package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record HomeProductResponse(
		Long id,
		Long shopId,
		String name,
		String seriesName,
		String gameName,
		BigDecimal price,
		String imageUrl,
		Integer soldCount) {
}
