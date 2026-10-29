package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record WishlistProductResponse(
		Long wishlistId,
		Long productId,
		String name,
		String shopName,
		BigDecimal price,
		String imageUrl,
		Integer favoriteCount,
		Integer soldCount) {
}
