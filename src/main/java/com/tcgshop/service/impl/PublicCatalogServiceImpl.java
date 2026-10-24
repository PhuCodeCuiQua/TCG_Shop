package com.tcgshop.service.impl;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicCatalogProductResponse;
import com.tcgshop.dto.response.PublicSeriesPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PublicCatalogProductMapper;
import com.tcgshop.repository.PublicCatalogProductRepository;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PublicCatalogService;
import com.tcgshop.service.SeriesCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicCatalogServiceImpl implements PublicCatalogService {
	private static final String GAME_NOT_FOUND_MESSAGE = "Không tìm thấy game";
	private static final String SERIES_NOT_FOUND_MESSAGE = "Không tìm thấy series";

	private final GameCatalogService gameCatalogService;
	private final SeriesCatalogService seriesCatalogService;
	private final PublicCatalogProductRepository publicCatalogProductRepository;
	private final PublicCatalogProductMapper publicCatalogProductMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<GameResponse> getActiveGames(Pageable pageable) {
		return gameCatalogService.search(null, true, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public GameResponse getActiveGame(Long gameId) {
		GameResponse game = gameCatalogService.getById(gameId);
		if (!game.active()) {
			throw new ResourceNotFoundException(GAME_NOT_FOUND_MESSAGE);
		}
		return game;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<SeriesResponse> getActiveSeries(Long gameId, Pageable pageable) {
		getActiveGame(gameId);
		return seriesCatalogService.search(null, gameId, true, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public PublicSeriesPageResponse getActiveSeriesProducts(Long seriesId, Pageable pageable) {
		SeriesResponse series = seriesCatalogService.getById(seriesId);
		if (!series.active()) {
			throw new ResourceNotFoundException(SERIES_NOT_FOUND_MESSAGE);
		}
		GameResponse game = getActiveGame(series.gameId());
		PageResponse<PublicCatalogProductResponse> products = PageResponse.from(
				publicCatalogProductRepository.findPublicProductsBySeriesId(seriesId, pageable),
				publicCatalogProductMapper::toResponse);
		return new PublicSeriesPageResponse(game, series, products);
	}
}
