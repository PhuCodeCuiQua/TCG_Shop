package com.tcgshop.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.mapper.PublicProductSearchMapper;
import com.tcgshop.repository.ProductRepository;
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
				1L, null, 1L, true, null, null, "Card", "A-01", "Game", "Series",
				com.tcgshop.entity.enums.ProductType.SINGLE, null, null,
				new BigDecimal("1000"), 2L, 20L, new BigDecimal("4.50"),
				6L, 8L, null);
		PublicProductSearchItemResponse response = new PublicProductSearchItemResponse(
				1L, null, 1L, true, null, null, "Card", "A-01", "Game", "Series",
				com.tcgshop.entity.enums.ProductType.SINGLE, null, null,
				new BigDecimal("1000"), 2L, 20L, new BigDecimal("4.50"),
				6L, 8L, null);
		when(productRepository.searchPublic(any(), eq(PageRequest.of(0, 20)),
				eq(PublicProductSearchRequest.SortBy.FAVORITES), eq(true)))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));
		when(publicProductSearchMapper.toResponse(projection)).thenReturn(response);

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
}
