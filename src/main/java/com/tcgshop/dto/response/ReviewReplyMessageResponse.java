package com.tcgshop.dto.response;

import java.time.LocalDateTime;

public record ReviewReplyMessageResponse(
		Long id,
		Long senderId,
		String senderName,
		String senderRole,
		String content,
		LocalDateTime createdAt) {
}
