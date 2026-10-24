package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import java.math.BigDecimal;

public record PublicProductSearchItemResponse(
		Long itemId,
		Long productId,
		Long cardId,
		boolean singleCard,
		Long shopId,
		String shopName,
		String name,
		String cardNumber,
		String gameName,
		String seriesName,
		ProductType type,
		CardLanguage language,
		Rarity rarity,
		BigDecimal price,
		Long sellerCount,
		Long soldCount,
		BigDecimal avgRating,
		Long ratingCount,
		Long favoriteCount,
		String imageUrl) {
}
