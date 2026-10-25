package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

public record PublicCatalogProductResponse(
		Long id,
		Long shopId,
		String shopName,
		String name,
		String cardName,
		String cardNumber,
		ProductType type,
		CardCondition cardCondition,
		BigDecimal price,
		Integer stock,
		Integer soldCount,
		String imageUrl) {
}
