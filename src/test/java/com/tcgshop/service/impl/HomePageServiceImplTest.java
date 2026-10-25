package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.HomeProductResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.mapper.HomePageMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.HomeProductProjection;
import com.tcgshop.repository.HomeProductRepository;
import com.tcgshop.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class HomePageServiceImplTest {

	private GameRepository gameRepository;
	private HomeProductRepository homeProductRepository;
	private HomePageMapper homePageMapper;
	private CurrentUser currentUser;
	private HomePageServiceImpl service;

	@BeforeEach
	void setUp() {
		gameRepository = mock(GameRepository.class);
		homeProductRepository = mock(HomeProductRepository.class);
		homePageMapper = mock(HomePageMapper.class);
		currentUser = mock(CurrentUser.class);
		service = new HomePageServiceImpl(
				gameRepository, homeProductRepository, homePageMapper, currentUser);
		when(gameRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
		when(homeProductRepository.findBestSellers(any(Pageable.class))).thenReturn(List.of());
	}

	@Test
	void guestGetsPopularProductsAndDoesNotLoadUserSpecificContent() {
		HomeProductProjection bestseller = projection(1L);
		when(homeProductRepository.findBestSellers(any(Pageable.class))).thenReturn(List.of(bestseller));
		when(homePageMapper.toProductResponse(bestseller)).thenReturn(product(1L));
		when(currentUser.isAuthenticated()).thenReturn(false);

		var response = service.getHomePage();

		assertThat(response.bestSellers()).extracting(HomeProductResponse::id).containsExactly(1L);
		assertThat(response.recentlyViewed()).isEmpty();
		assertThat(response.recommendations()).isEmpty();
		verify(homeProductRepository, never()).findRecentlyViewedByUserId(any(), any(Pageable.class));
		verify(homeProductRepository, never()).findRecommendedProducts(any(Pageable.class));
	}

	@Test
	void userGetsRecentProductsAndRecommendationsExcludeBestsellers() {
		HomeProductProjection bestseller = projection(1L);
		HomeProductProjection recent = projection(2L);
		HomeProductProjection duplicateRecommendation = projection(1L);
		HomeProductProjection recommendation = projection(3L);
		when(homeProductRepository.findBestSellers(any(Pageable.class))).thenReturn(List.of(bestseller));
		when(homeProductRepository.findRecentlyViewedByUserId(org.mockito.ArgumentMatchers.eq(42L),
				any(Pageable.class))).thenReturn(List.of(recent));
		when(homeProductRepository.findRecommendedProducts(any(Pageable.class)))
				.thenReturn(List.of(duplicateRecommendation, recommendation));
		when(homePageMapper.toProductResponse(bestseller)).thenReturn(product(1L));
		when(homePageMapper.toProductResponse(recent)).thenReturn(product(2L));
		when(homePageMapper.toProductResponse(recommendation)).thenReturn(product(3L));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(42L);

		var response = service.getHomePage();

		assertThat(response.recentlyViewed()).extracting(HomeProductResponse::id).containsExactly(2L);
		assertThat(response.recommendations()).extracting(HomeProductResponse::id).containsExactly(3L);
		verify(homeProductRepository).findRecentlyViewedByUserId(org.mockito.ArgumentMatchers.eq(42L),
				any(Pageable.class));
	}

	private HomeProductProjection projection(Long id) {
		HomeProductProjection projection = mock(HomeProductProjection.class);
		when(projection.getId()).thenReturn(id);
		return projection;
	}

	private HomeProductResponse product(Long id) {
		return new HomeProductResponse(id, 10L, "Product " + id, "Series", "Game",
				null, null, 20);
	}
}
