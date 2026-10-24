package com.tcgshop.controller.web;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PublicSeriesPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.service.PublicCatalogService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PublicCatalogController {
	private static final int GAME_PAGE_SIZE = 12;
	private static final int SERIES_PAGE_SIZE = 12;
	private static final int PRODUCT_PAGE_SIZE = 12;

	private final PublicCatalogService publicCatalogService;

	@GetMapping("/games")
	public String games(@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), GAME_PAGE_SIZE, Sort.by("name").ascending());
		model.addAttribute("gamesPage", publicCatalogService.getActiveGames(pageable));
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Trang chủ", "url", "/"),
				Map.of("label", "Game", "url", "/games")));
		return "guest/games";
	}

	@GetMapping("/games/{gameId}")
	public String series(
			@PathVariable Long gameId,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		GameResponse game = publicCatalogService.getActiveGame(gameId);
		Pageable pageable = PageRequest.of(Math.max(page, 0), SERIES_PAGE_SIZE, Sort.by("name").ascending());
		model.addAttribute("game", game);
		model.addAttribute("seriesPage", publicCatalogService.getActiveSeries(gameId, pageable));
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Trang chủ", "url", "/"),
				Map.of("label", "Game", "url", "/games"),
				Map.of("label", game.name(), "url", "/games/" + gameId)));
		return "guest/game-series";
	}

	@GetMapping("/series/{seriesId}")
	public String products(
			@PathVariable Long seriesId,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PRODUCT_PAGE_SIZE, Sort.unsorted());
		PublicSeriesPageResponse response = publicCatalogService.getActiveSeriesProducts(seriesId, pageable);
		GameResponse game = response.game();
		SeriesResponse series = response.series();
		model.addAttribute("game", game);
		model.addAttribute("series", series);
		model.addAttribute("productsPage", response.products());
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Trang chủ", "url", "/"),
				Map.of("label", "Game", "url", "/games"),
				Map.of("label", game.name(), "url", "/games/" + game.id()),
				Map.of("label", series.name(), "url", "/series/" + series.id())));
		return "guest/series-products";
	}
}
