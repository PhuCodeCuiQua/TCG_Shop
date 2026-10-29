package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlatformPromotionResponse(
		Long id,
		String title,
		PromotionType type,
		BigDecimal discountPercent,
		BigDecimal discountAmount,
		BigDecimal minOrderValue,
		Long gameId,
		String gameName,
		Long seriesId,
		String seriesName,
		List<Long> productIds,
		String targetSummary,
		LocalDateTime startDate,
		LocalDateTime endDate,
		ActiveStatus status) {
}
