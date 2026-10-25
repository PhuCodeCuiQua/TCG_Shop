package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ReviewSummaryResponse(BigDecimal averageRating, int reviewCount) {
}
