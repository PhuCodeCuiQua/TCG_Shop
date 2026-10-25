package com.tcgshop.dto.response;

import java.util.List;

public record HomePageResponse(
		List<HomeGameResponse> games,
		List<HomeProductResponse> bestSellers,
		List<HomeProductResponse> recentlyViewed,
		List<HomeProductResponse> recommendations) {
}
