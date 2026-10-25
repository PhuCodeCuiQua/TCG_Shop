package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PriceUpdateResponse(
		Long cardId,
		String cardName,
		BigDecimal oldPrice,
		BigDecimal newPrice,
		BigDecimal changePercent,
		LocalDate recordDate,
		String source,
		LocalDateTime updatedAt) {
}
