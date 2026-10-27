package com.tcgshop.repository;

import java.math.BigDecimal;

public interface ChatbotPriceCandidateProjection {
	Long getProductId();
	String getCardName();
	String getImageUrl();
	String getShopName();
	BigDecimal getPrice();
}
