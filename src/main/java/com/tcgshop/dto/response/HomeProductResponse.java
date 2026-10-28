package com.tcgshop.dto.response;

import java.math.BigDecimal;
import com.tcgshop.entity.enums.ProductType;

public record HomeProductResponse(
		Long id,
		Long shopId,
		String name,
		String seriesName,
		String gameName,
		BigDecimal price,
		BigDecimal originalPrice,
		BigDecimal effectivePrice,
		String imageUrl,
		Integer soldCount,
		ProductType type) {

	public HomeProductResponse(
			Long id,
			Long shopId,
			String name,
			String seriesName,
			String gameName,
			BigDecimal price,
			String imageUrl,
			Integer soldCount) {
		this(id, shopId, name, seriesName, gameName, price, price, price, imageUrl, soldCount, null);
	}

	public HomeProductResponse(
			Long id,
			Long shopId,
			String name,
			String seriesName,
			String gameName,
			BigDecimal price,
			BigDecimal originalPrice,
			BigDecimal effectivePrice,
			String imageUrl,
			Integer soldCount) {
		this(id, shopId, name, seriesName, gameName, price, originalPrice, effectivePrice,
				imageUrl, soldCount, null);
	}
}
