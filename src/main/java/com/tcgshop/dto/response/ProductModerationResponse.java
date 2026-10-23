package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import java.time.LocalDateTime;

public record ProductModerationResponse(
		Long id,
		String name,
		Long shopId,
		String shopName,
		ProductType type,
		ProductStatus status,
		Integer stock,
		String banReason,
		LocalDateTime createdAt) {
}
