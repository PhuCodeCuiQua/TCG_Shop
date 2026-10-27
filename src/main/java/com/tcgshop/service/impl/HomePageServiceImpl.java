package com.tcgshop.service.impl;

import com.tcgshop.dto.response.HomePageResponse;
import com.tcgshop.dto.response.HomeProductResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.mapper.HomePageMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.HomeProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.HomePageService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomePageServiceImpl implements HomePageService {
	private static final int BEST_SELLER_LIMIT = 12;
	private static final int HERO_IMAGE_LIMIT = 4;
	private static final int SUPPLIES_AND_SEALED_LIMIT = 8;
	private static final int RECENTLY_VIEWED_LIMIT = 8;
	private static final int RECOMMENDATION_LIMIT = 8;
	private static final int RECOMMENDATION_CANDIDATE_LIMIT = 24;

	private final GameRepository gameRepository;
	private final HomeProductRepository homeProductRepository;
	private final HomePageMapper homePageMapper;
	private final CurrentUser currentUser;
	private final com.tcgshop.repository.SeriesRepository seriesRepository;

	public HomePageServiceImpl(
			GameRepository gameRepository,
			HomeProductRepository homeProductRepository,
			HomePageMapper homePageMapper,
			CurrentUser currentUser, com.tcgshop.repository.SeriesRepository seriesRepository) {
		this.gameRepository = gameRepository;
		this.homeProductRepository = homeProductRepository;
		this.homePageMapper = homePageMapper;
		this.currentUser = currentUser;
		this.seriesRepository = seriesRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public HomePageResponse getHomePage() {
		List<HomeProductResponse> bestSellers =
				homeProductRepository.findBestSellers(PageRequest.of(0, BEST_SELLER_LIMIT))
						.stream()
						.map(homePageMapper::toProductResponse)
						.toList();

		boolean isUser = currentUser.isAuthenticated() && currentUser.hasRole(RoleName.ROLE_USER);
		List<HomeProductResponse> recentlyViewed = isUser
				? homeProductRepository.findRecentlyViewedByUserId(
						currentUser.getId(), PageRequest.of(0, RECENTLY_VIEWED_LIMIT))
						.stream()
						.map(homePageMapper::toProductResponse)
						.toList()
				: List.of();
		List<HomeProductResponse> recommendations = isUser
				? findRecommendations(bestSellers)
				: List.of();

		return new HomePageResponse(
				gameRepository.findByActiveTrueOrderByNameAsc().stream()
						.map(homePageMapper::toGameResponse)
						.toList(),
				bestSellers,
				recentlyViewed,
				recommendations,
				homeProductRepository.findSuppliesAndSealed(PageRequest.of(0, SUPPLIES_AND_SEALED_LIMIT)).stream()
						.map(homePageMapper::toProductResponse).toList(),
				findLatestSets(),
				homeProductRepository.findHeroImages(HERO_IMAGE_LIMIT).stream()
						.map(com.tcgshop.repository.HeroImageProjection::getImageUrl)
						.filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
						.toList());
	}

	private List<com.tcgshop.dto.response.HomeLatestSetResponse> findLatestSets() {
		java.util.Map<Long, com.tcgshop.entity.Series> selectedSets = new java.util.LinkedHashMap<>();
		seriesRepository.findFeaturedYuGiOhSets().forEach(set -> selectedSets.put(set.getId(), set));
		seriesRepository.findLatestActive(PageRequest.of(0, 4)).forEach(set -> {
			if (selectedSets.size() < 4) selectedSets.putIfAbsent(set.getId(), set);
		});
		var sets = selectedSets.values().stream().limit(4).toList();
		if (sets.isEmpty()) return List.of();
		java.util.Map<Long, String> covers = new java.util.HashMap<>();
		// Xếp hạng giá theo từng Series trong một truy vấn, không lấy ảnh thẻ rẻ hơn nếu thẻ đắt nhất thiếu ảnh.
		homeProductRepository.findSeriesCovers(sets.stream().map(com.tcgshop.entity.Series::getId).toList())
				.forEach(row -> covers.put(row.getSeriesId(), row.getImageUrl()));
		return sets.stream().map(homePageMapper::toLatestSetResponse)
				.map(set -> new com.tcgshop.dto.response.HomeLatestSetResponse(set.id(), set.name(), set.gameName(),
						set.releaseDate(), set.logoUrl(), covers.get(set.id()))).toList();
	}

	private List<HomeProductResponse> findRecommendations(List<HomeProductResponse> bestSellers) {
		List<Long> bestSellerIds = bestSellers.stream()
				.map(HomeProductResponse::id)
				.toList();
		return homeProductRepository.findRecommendedProducts(PageRequest.of(0, RECOMMENDATION_CANDIDATE_LIMIT))
				.stream()
				.filter(projection -> !bestSellerIds.contains(projection.getId()))
				.limit(RECOMMENDATION_LIMIT)
				.map(homePageMapper::toProductResponse)
				.toList();
	}
}
