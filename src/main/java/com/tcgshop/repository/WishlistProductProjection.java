package com.tcgshop.repository;

import java.math.BigDecimal;

public interface WishlistProductProjection {
	Long getWishlistId();

	Long getProductId();

	String getName();

	String getShopName();

	BigDecimal getPrice();

	String getImageUrl();

	Integer getFavoriteCount();

	Integer getSoldCount();
}
