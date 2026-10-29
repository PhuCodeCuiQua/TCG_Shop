package com.tcgshop.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
		Long id,
		Long reviewerId,
		String reviewerName,
		String reviewerAvatarUrl,
		int rating,
		String comment,
		LocalDateTime createdAt,
		List<ReviewMediaResponse> media,
		List<ReviewReplyMessageResponse> replies) {
}
