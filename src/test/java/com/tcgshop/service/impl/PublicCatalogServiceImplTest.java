package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicCatalogProductResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PublicCatalogProductMapper;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.repository.PublicCatalogProductRepository;
import com.tcgshop.repository.PublicCatalogProductProjection;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PromotionService;
import com.tcgshop.service.SeriesCatalogService;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PublicCatalogServiceImplTest {

	private GameCatalogService gameCatalogService;
	private SeriesCatalogService seriesCatalogService;
	private PublicCatalogProductRepository productRepository;
	private ProductImageRepository productImageRepository;
	private PublicCatalogProductMapper productMapper;
	private PromotionService promotionService;
	private PublicCatalogServiceImpl service;

	@BeforeEach
	void setUp() {
		gameCatalogService = mock(GameCatalogService.class);
		seriesCatalogService = mock(SeriesCatalogService.class);
		productRepository = mock(PublicCatalogProductRepository.class);
		productImageRepository = mock(ProductImageRepository.class);
		productMapper = mock(PublicCatalogProductMapper.class);
		promotionService = mock(PromotionService.class);
		service = new PublicCatalogServiceImpl(
				gameCatalogService, seriesCatalogService, productRepository, productImageRepository,
				productMapper, promotionService);
		when(promotionService.getEffectivePrices(any())).thenReturn(Map.of());
	}

	@Test
	void inactiveGameIsNotExposedThroughPublicCatalog() {
		when(gameCatalogService.getById(5L))
				.thenReturn(new GameResponse(5L, "Hidden", "H", null, null, false, null));

		assertThatThrownBy(() -> service.getActiveSeries(5L, Pageable.unpaged()))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(seriesCatalogService, never()).search(any(), any(), any(), any(Pageable.class));
	}

	@Test
	void seriesProductPageRequiresVisibleSeriesAndGame() {
		when(seriesCatalogService.getById(8L))
				.thenReturn(new SeriesResponse(8L, 5L, "Game", "Series", "S", null, false));

		assertThatThrownBy(() -> service.getActiveSeriesProducts(8L, Pageable.unpaged()))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(gameCatalogService, never()).getById(5L);
		verify(productRepository, never()).findPublicProductsBySeriesId(any(), any());
	}

	@Test
	void publicSeriesPageMapsProductProjectionIntoResponse() {
		GameResponse game = new GameResponse(5L, "Game", "G", null, null, true, null);
		SeriesResponse series = new SeriesResponse(8L, 5L, "Game", "Series", "S", null, true);
		PublicCatalogProductProjection projection = mock(PublicCatalogProductProjection.class);
		PublicCatalogProductResponse product = new PublicCatalogProductResponse(
				1L, 2L, "Shop", "Product", null, null, ProductType.SINGLE,
				null, new BigDecimal("1000.00"), 5, 0, null);
		when(seriesCatalogService.getById(8L)).thenReturn(series);
		when(gameCatalogService.getById(5L)).thenReturn(game);
		when(productRepository.findPublicProductsBySeriesId(any(), any())).thenReturn(
				new org.springframework.data.domain.PageImpl<>(List.of(projection)));
		when(productMapper.toResponse(projection)).thenReturn(product);
		when(promotionService.getEffectivePrices(List.of(1L)))
				.thenReturn(Map.of(1L, new BigDecimal("800.00")));
		when(productImageRepository.findThumbnails(any())).thenReturn(List.of());
		when(productImageRepository.findSeriesFallbacks(any())).thenReturn(List.of());

		var response = service.getActiveSeriesProducts(8L, Pageable.unpaged());

		assertThat(response.game()).isEqualTo(game);
		assertThat(response.series()).isEqualTo(series);
		assertThat(response.products().content()).hasSize(1);
		assertThat(response.products().content().getFirst().originalPrice()).isEqualByComparingTo("1000.00");
		assertThat(response.products().content().getFirst().effectivePrice()).isEqualByComparingTo("800.00");
	}
}
