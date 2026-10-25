package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceChartPointResponse(LocalDate date, BigDecimal marketPrice, BigDecimal avgPrice) {
}