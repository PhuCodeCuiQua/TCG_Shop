package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
		Long id,
		String name,
		String description,
		String imageUrl,
		boolean singleCard,
		Long cardId,
		String seriesName,
		String gameName,
		String cardNumber,
		String rarity,
		String condition,
		String language,
		BigDecimal price,
		BigDecimal effectivePrice,
		int stock,
		boolean inStock,
		int soldCount,
		BigDecimal marketPrice,
		BigDecimal lowestPrice,
		Long shopId,
		String shopName,
		List<OfferItem> offers) {

	/** Một người bán đang bán cùng thẻ (dùng cho ô chọn người bán). */
	public record OfferItem(
			Long productId,
			Long shopId,
			String shopName,
			String condition,
			String language,
			BigDecimal price,
			BigDecimal effectivePrice,
			int stock) {
	}
}