package com.tcgshop.dto.response;

import java.util.List;

public record PublicProductSearchOptionsResponse(
		List<GameResponse> games,
		List<SeriesResponse> series) {
}
