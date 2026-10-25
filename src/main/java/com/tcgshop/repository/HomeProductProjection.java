package com.tcgshop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface HomeProductProjection {
	Long getId();

	Long getShopId();

	String getName();

	String getSeriesName();

	String getGameName();

	BigDecimal getPrice();

	String getImageUrl();

	Integer getSoldCount();

	LocalDateTime getViewedAt();

	BigDecimal getAvgRating();

	Integer getRatingCount();
}
