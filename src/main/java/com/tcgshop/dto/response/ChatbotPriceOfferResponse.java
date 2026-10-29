package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ChatbotPriceOfferResponse(Long productId, String cardName, String imageUrl,
		String shopName, BigDecimal price) { }
