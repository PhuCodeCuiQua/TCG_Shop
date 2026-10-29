package com.tcgshop.service;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.GameSeriesResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicSeriesPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import org.springframework.data.domain.Pageable;

public interface PublicCatalogService {

	PageResponse<GameResponse> getActiveGames(Pageable pageable);

	GameResponse getActiveGame(Long gameId);

	PageResponse<GameSeriesResponse> getActiveSeries(Long gameId, Pageable pageable);

	PublicSeriesPageResponse getActiveSeriesProducts(Long seriesId, Pageable pageable);
}
