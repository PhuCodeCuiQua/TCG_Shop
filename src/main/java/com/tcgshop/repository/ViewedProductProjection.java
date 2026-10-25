package com.tcgshop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ViewedProductProjection {
	Long getViewedId();

	Long getProductId();

	Long getShopId();

	String getShopName();

	String getName();

	String getSeriesName();

	String getGameName();

	BigDecimal getPrice();

	String getImageUrl();

	Integer getSoldCount();

	LocalDateTime getViewedAt();
}
