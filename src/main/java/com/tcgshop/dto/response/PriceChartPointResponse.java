package com.tcgshop.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceChartPointResponse(
		@JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date,
		BigDecimal marketPrice,
		BigDecimal avgPrice) {
}