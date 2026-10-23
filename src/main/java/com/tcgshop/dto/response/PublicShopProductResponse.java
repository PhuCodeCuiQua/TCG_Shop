package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

public record PublicShopProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		Integer stock,
		ProductType type,
		String imageUrl) {
}
