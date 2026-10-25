package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * points rỗng nghĩa là chưa có dữ liệu giá trong kỳ.
 * changePercent = null khi không có mốc so sánh; comparedToPreviousPeriod = false nghĩa là
 * mốc so sánh là điểm đầu kỳ (do kỳ trước không có dữ liệu).
 */
public record PriceChartResponse(
		Long cardId,
		int range,
		LocalDate from,
		LocalDate to,
		List<PriceChartPointResponse> points,
		BigDecimal currentPrice,
		BigDecimal previousPrice,
		BigDecimal changePercent,
		boolean comparedToPreviousPeriod) {
}