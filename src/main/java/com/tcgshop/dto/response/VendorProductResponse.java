package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorProductResponse(
		Long id,
		String name,
		ProductType type,
		BigDecimal price,
		Integer stock,
		ProductStatus status,
		String imageUrl,
		LocalDateTime createdAt) {
}
