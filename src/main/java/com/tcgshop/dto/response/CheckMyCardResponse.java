package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckMyCardResponse(
		Long cardId,
		String cardName,
		String cardNumber,
		String imageUrl,
		BigDecimal marketPrice,
		BigDecimal lowestSellingPrice,
		long sellerCount,
		BigDecimal priceDifferencePercent,
		Long cheapestProductId,
		List<PriceChartPointResponse> chartPoints) {
}
