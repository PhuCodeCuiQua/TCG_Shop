package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record OrderItemPreviewResponse(Long productId, String productName, String imageUrl,
		Integer quantity, BigDecimal price) { }
