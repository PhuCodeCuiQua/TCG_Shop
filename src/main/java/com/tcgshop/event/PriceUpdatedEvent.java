package com.tcgshop.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sự kiện phát sau khi giá thị trường của thẻ được cập nhật. */
public record PriceUpdatedEvent(
		Long cardId,
		String cardName,
		BigDecimal oldPrice,
		BigDecimal newPrice,
		LocalDate recordDate,
		String source) {
}
