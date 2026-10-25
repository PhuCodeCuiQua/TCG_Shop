package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShipperStatisticsResponse(
		LocalDate fromDate,
		LocalDate toDate,
		long deliveredCount,
		long deliveryFailedCount,
		long inProgressCount,
		BigDecimal successRate,
		BigDecimal codCollectedTotal) {
}
