package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

public record PublicShopProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		BigDecimal originalPrice,
		BigDecimal effectivePrice,
		Integer stock,
		ProductType type,
		String imageUrl) {

	public PublicShopProductResponse(
			Long id,
			String name,
			String description,
			BigDecimal price,
			Integer stock,
			ProductType type,
			String imageUrl) {
		this(id, name, description, price, price, price, stock, type, imageUrl);
	}
}
