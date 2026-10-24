package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendorPromotionResponse(
		Long id,
		String title,
		BigDecimal discountPercent,
		LocalDateTime startDate,
		LocalDateTime endDate,
		ActiveStatus status,
		String displayStatus,
		List<Long> productIds) { }
