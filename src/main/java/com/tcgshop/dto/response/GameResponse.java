package com.tcgshop.dto.response;

import java.time.LocalDateTime;

public record GameResponse(
		Long id,
		String name,
		String code,
		String description,
		String logoUrl,
		boolean active,
		LocalDateTime createdAt) {
}