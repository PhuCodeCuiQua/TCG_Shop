package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import java.math.BigDecimal;

/** Thẻ gốc trả về cho trang quản lý. Không có imagePublicId vì client không cần. */
public record CardResponse(
		Long id,
		Long seriesId,
		String seriesName,
		Long gameId,
		String gameName,
		String name,
		String cardNumber,
		Rarity rarity,
		CardLanguage language,
		String imageUrl,
		BigDecimal marketPrice,
		boolean active) {
}