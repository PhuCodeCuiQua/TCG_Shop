package com.tcgshop.dto.response;

import java.util.List;

public record PublicProductSearchOptionsResponse(
		List<GameResponse> games,
		List<SeriesResponse> series,
		List<PublicShopOptionResponse> shops) {

	public PublicProductSearchOptionsResponse(List<GameResponse> games, List<SeriesResponse> series) {
		this(games, series, List.of());
	}
}
