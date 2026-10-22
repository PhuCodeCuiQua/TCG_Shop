package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
		Long id,
		NotificationType type,
		String typeDisplayName,
		String title,
		String content,
		String link,
		boolean read,
		LocalDateTime createdAt) {
}