package com.tcgshop.repository;

import java.math.BigDecimal;

public interface AdminMarketPrice {
	Long getCardId();

	BigDecimal getMarketPrice();
}
