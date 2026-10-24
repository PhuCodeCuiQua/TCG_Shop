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
	private static final int RECENTLY_VIEWED_LIMIT = 8;
	private static final int RECOMMENDATION_LIMIT = 8;
	private static final int RECOMMENDATION_CANDIDATE_LIMIT = 24;

	private final GameRepository gameRepository;
	private final HomeProductRepository homeProductRepository;
	private final HomePageMapper homePageMapper;
	private final CurrentUser currentUser;

	public HomePageServiceImpl(
			GameRepository gameRepository,
			HomeProductRepository homeProductRepository,
			HomePageMapper homePageMapper,
			CurrentUser currentUser) {
		this.gameRepository = gameRepository;
		this.homeProductRepository = homeProductRepository;
		this.homePageMapper = homePageMapper;
		this.currentUser = currentUser;
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
				recommendations);
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
