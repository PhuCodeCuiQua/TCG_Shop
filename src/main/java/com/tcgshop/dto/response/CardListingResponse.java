package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;

/** Dữ liệu gọn để Vendor tìm và chọn thẻ khi đăng bán (PA-11, giao sớm cho Phong). */
public record CardListingResponse(
		Long id,
		String name,
		String cardNumber,
		String seriesName,
		Rarity rarity,
		CardLanguage language,
		String imageUrl) {
}