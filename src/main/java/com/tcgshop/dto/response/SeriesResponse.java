package com.tcgshop.dto.response;

import java.time.LocalDate;

public record SeriesResponse(
		Long id,
		Long gameId,
		String gameName,
		String name,
		String code,
		LocalDate releaseDate,
		boolean active) {
}