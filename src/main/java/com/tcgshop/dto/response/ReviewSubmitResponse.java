package com.tcgshop.dto.response;

public record ReviewSubmitResponse(
		Long reviewId,
		ReviewSummaryResponse summary,
		String message) {
}
