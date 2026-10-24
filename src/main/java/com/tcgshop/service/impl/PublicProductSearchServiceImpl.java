package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.dto.response.PublicProductSearchOptionsResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.mapper.PublicProductSearchMapper;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.PublicProductSpecifications;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PublicProductSearchService;
import com.tcgshop.service.SeriesCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProductSearchServiceImpl implements PublicProductSearchService {

	private static final int PAGE_SIZE = 20;
	private static final int FILTER_OPTIONS_PAGE_SIZE = 1000;

	private final ProductRepository productRepository;
	private final PublicProductSearchMapper publicProductSearchMapper;
	private final GameCatalogService gameCatalogService;
	private final SeriesCatalogService seriesCatalogService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PublicProductSearchItemResponse> search(PublicProductSearchRequest filters) {
		boolean topTwenty = filters.isTopTwenty();
		PublicProductSearchRequest.SortBy sort = topTwenty
				? filters.getTab().getSortBy()
				: filters.getSort();
		int page = topTwenty ? 0 : filters.getPage();
		Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.unsorted());
		return PageResponse.from(
				productRepository.searchPublic(
						PublicProductSpecifications.from(filters), pageable, sort, topTwenty),
				publicProductSearchMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public PublicProductSearchOptionsResponse getFilterOptions(Long gameId) {
		Pageable optionsPage = PageRequest.of(0, FILTER_OPTIONS_PAGE_SIZE, Sort.by("name").ascending());
		List<GameResponse> games = gameCatalogService.search(null, true, optionsPage).content();
		List<SeriesResponse> series = seriesCatalogService.search(null, gameId, true, optionsPage).content();
		return new PublicProductSearchOptionsResponse(games, series);
	}
}
