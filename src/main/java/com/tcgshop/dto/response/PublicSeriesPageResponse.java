package com.tcgshop.dto.response;

public record PublicSeriesPageResponse(
		GameResponse game,
		SeriesResponse series,
		PageResponse<PublicCatalogProductResponse> products) {
}
