package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceHistoryResponse(
		Long id,
		Long cardId,
		String cardName,
		String cardNumber,
		String seriesName,
		LocalDate recordedDate,
		BigDecimal marketPrice,
		BigDecimal avgPrice,
		String source) {
}
