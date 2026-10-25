package com.tcgshop.dto.response;

import java.math.BigDecimal;

/** Một sản phẩm của một shop đang bán cùng một thẻ. shopRating = null nếu shop chưa có đánh giá. */
public record CardOfferResponse(
		Long productId,
		Long shopId,
		String shopName,
		BigDecimal shopRating,
		long shopRatingCount,
		String condition,
		String language,
		BigDecimal price,
		BigDecimal effectivePrice,
		int stock) {
}