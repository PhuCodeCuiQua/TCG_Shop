package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record OrderDetailItemResponse(Long productId, String productName, String imageUrl,
		String cardCondition, BigDecimal price, Integer quantity, BigDecimal lineTotal) { }
