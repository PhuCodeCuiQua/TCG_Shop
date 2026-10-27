package com.tcgshop.dto.response;

import java.util.List;

public record HomePageResponse(
		List<HomeGameResponse> games,
		List<HomeProductResponse> bestSellers,
		List<HomeProductResponse> recentlyViewed,
		List<HomeProductResponse> recommendations,
		List<HomeProductResponse> suppliesAndSealed,
		List<HomeLatestSetResponse> latestSets,
		List<String> heroImages) {
	public HomePageResponse(List<HomeGameResponse> games, List<HomeProductResponse> bestSellers,
			List<HomeProductResponse> recentlyViewed, List<HomeProductResponse> recommendations,
			List<HomeProductResponse> suppliesAndSealed, List<HomeLatestSetResponse> latestSets) {
		this(games, bestSellers, recentlyViewed, recommendations, suppliesAndSealed, latestSets, List.of());
	}
	public HomePageResponse(List<HomeGameResponse> games, List<HomeProductResponse> bestSellers,
			List<HomeProductResponse> recentlyViewed, List<HomeProductResponse> recommendations,
			List<HomeProductResponse> suppliesAndSealed) {
		this(games, bestSellers, recentlyViewed, recommendations, suppliesAndSealed, List.of(), List.of());
	}
	public HomePageResponse(List<HomeGameResponse> games, List<HomeProductResponse> bestSellers,
			List<HomeProductResponse> recentlyViewed, List<HomeProductResponse> recommendations) {
		this(games, bestSellers, recentlyViewed, recommendations, List.of(), List.of(), List.of());
	}
}
