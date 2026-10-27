package com.tcgshop.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.mapper.PublicProductSearchMapper;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.repository.ProductThumbnailProjection;
import com.tcgshop.repository.PublicProductSearchProjection;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.SeriesCatalogService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PublicProductSearchServiceImplTest {

	@Mock private ProductRepository productRepository;
	@Mock private ProductImageRepository productImageRepository;
	@Mock private PublicProductSearchMapper publicProductSearchMapper;
	@Mock private GameCatalogService gameCatalogService;
	@Mock private SeriesCatalogService seriesCatalogService;
	@InjectMocks private PublicProductSearchServiceImpl publicProductSearchService;

	@Test
	void rankingTabsAlwaysReturnOnlyTheFirstTwentyItems() {
		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setPage(4);
		filters.setTab(PublicProductSearchRequest.TopTab.FAVORITES);
		PublicProductSearchProjection projection = new PublicProductSearchProjection(
				1L, 1L, 1L, true, null, null, "Card", "A-01", "Game", "Series",
				com.tcgshop.entity.enums.ProductType.SINGLE, null, null,
				new BigDecimal("1000"), 2L, 20L, new BigDecimal("4.50"),
				6L, 8L, null);
		PublicProductSearchItemResponse response = new PublicProductSearchItemResponse(
				1L, 1L, 1L, true, null, null, "Card", "A-01", "Game", "Series",
				com.tcgshop.entity.enums.ProductType.SINGLE, null, null,
				new BigDecimal("1000"), 2L, 20L, new BigDecimal("4.50"),
				6L, 8L, null);
		when(productRepository.searchPublic(any(), eq(PageRequest.of(0, 20)),
				eq(PublicProductSearchRequest.SortBy.FAVORITES), eq(true)))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));
		when(publicProductSearchMapper.toResponse(projection)).thenReturn(response);
		when(productImageRepository.findThumbnails(anyCollection())).thenReturn(List.of());
		when(productImageRepository.findSeriesFallbacks(anyCollection())).thenReturn(List.of());

		publicProductSearchService.search(filters);

		verify(productRepository).searchPublic(any(), eq(PageRequest.of(0, 20)),
				eq(PublicProductSearchRequest.SortBy.FAVORITES), eq(true));
	}

	@Test
	void newestSearchSortRetainsTheRequestedPaginationInsteadOfUsingTopTwentyMode() {
		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setPage(3);
		filters.setSort(PublicProductSearchRequest.SortBy.NEWEST);
		when(productRepository.searchPublic(any(), eq(PageRequest.of(3, 20)),
				eq(PublicProductSearchRequest.SortBy.NEWEST), eq(false)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 20), 100));

		publicProductSearchService.search(filters);

		verify(productRepository).searchPublic(any(), eq(PageRequest.of(3, 20)),
				eq(PublicProductSearchRequest.SortBy.NEWEST), eq(false));
	}

	@Test
	void supplySearchUsesItsMainProductImageInsteadOfSeriesFallbacks() {
		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setSort(PublicProductSearchRequest.SortBy.NEWEST);
		PublicProductSearchProjection projection = new PublicProductSearchProjection(
				7L, 7L, null, false, 2L, "Shop", "Sleeves", null, null, null,
				com.tcgshop.entity.enums.ProductType.SUPPLY, null, null,
				new BigDecimal("50000"), 1L, 0L, null, 0L, 0L, null);
		PublicProductSearchItemResponse response = new PublicProductSearchItemResponse(
				7L, 7L, null, false, 2L, "Shop", "Sleeves", null, null, null,
				com.tcgshop.entity.enums.ProductType.SUPPLY, null, null,
				new BigDecimal("50000"), 1L, 0L, null, 0L, 0L, null);
		Product product = mock(Product.class);
		ProductImage mainImage = mock(ProductImage.class);
		when(product.getId()).thenReturn(7L);
		when(mainImage.getProduct()).thenReturn(product);
		when(mainImage.getImageUrl()).thenReturn("https://images.example/sleeves.jpg");
		when(productRepository.searchPublic(any(), eq(PageRequest.of(0, 20)),
				eq(PublicProductSearchRequest.SortBy.NEWEST), eq(false)))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));
		when(publicProductSearchMapper.toResponse(projection)).thenReturn(response);
		when(productImageRepository.findThumbnails(anyCollection())).thenReturn(List.of());
		when(productImageRepository.findSeriesFallbacks(anyCollection())).thenReturn(List.of());
		when(productImageRepository.findMainImages(List.of(7L))).thenReturn(List.of(mainImage));

		var result = publicProductSearchService.search(filters);

		org.junit.jupiter.api.Assertions.assertEquals(
				"https://images.example/sleeves.jpg", result.content().getFirst().imageUrl());
	}

	@Test
	void supplyWithoutMainImageDoesNotUseAnUnrelatedSeriesCardAsItsImage() {
		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setSort(PublicProductSearchRequest.SortBy.NEWEST);
		PublicProductSearchProjection projection = new PublicProductSearchProjection(
				7L, 7L, null, false, 2L, "Shop", "Sleeves", null, null, "Series",
				com.tcgshop.entity.enums.ProductType.SUPPLY, null, null,
				new BigDecimal("50000"), 1L, 0L, null, 0L, 0L, null);
		PublicProductSearchItemResponse response = new PublicProductSearchItemResponse(
				7L, 7L, null, false, 2L, "Shop", "Sleeves", null, null, "Series",
				com.tcgshop.entity.enums.ProductType.SUPPLY, null, null,
				new BigDecimal("50000"), 1L, 0L, null, 0L, 0L, "https://images.example/card.jpg");
		when(productRepository.searchPublic(any(), eq(PageRequest.of(0, 20)),
				eq(PublicProductSearchRequest.SortBy.NEWEST), eq(false)))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));
		when(publicProductSearchMapper.toResponse(projection)).thenReturn(response);
		when(productImageRepository.findThumbnails(anyCollection())).thenReturn(List.of());
		ProductThumbnailProjection seriesFallback = mock(ProductThumbnailProjection.class);
		when(seriesFallback.getProductId()).thenReturn(7L);
		when(seriesFallback.getImageUrl()).thenReturn("https://images.example/card.jpg");
		when(productImageRepository.findSeriesFallbacks(anyCollection())).thenReturn(List.of(seriesFallback));
		when(productImageRepository.findMainImages(List.of(7L))).thenReturn(List.of());

		var result = publicProductSearchService.search(filters);

		org.junit.jupiter.api.Assertions.assertNull(result.content().getFirst().imageUrl());
	}
}
