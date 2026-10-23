package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record PublicShopResponse(
		Long id,
		String name,
		String slug,
		String description,
		String logoUrl,
		String bannerUrl,
		String phoneNumber,
		String pickupAddress,
		BigDecimal avgRating,
		Integer ratingCount) {
}
