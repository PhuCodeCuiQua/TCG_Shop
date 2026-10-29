package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ReviewProductResponse(Long id, String name, BigDecimal averageRating, int reviewCount,
		String imageUrl, BigDecimal price) {
	public ReviewProductResponse(Long id, String name, BigDecimal averageRating, int reviewCount) {
		this(id, name, averageRating, reviewCount, null, null);
	}
}
