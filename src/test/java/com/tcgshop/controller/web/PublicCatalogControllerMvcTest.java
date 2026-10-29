package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.GameSeriesResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicCatalogProductResponse;
import com.tcgshop.dto.response.PublicSeriesPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.service.PublicCatalogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicCatalogControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@MockBean private PublicCatalogService publicCatalogService;

	@Test
	void gameMenuPageRendersGamesAndPagination() throws Exception {
		GameResponse game = new GameResponse(1L, "Pokémon", "PKM", null, null, true, null);
		when(publicCatalogService.getActiveGames(any(Pageable.class)))
				.thenReturn(page(List.of(game), 0, 2));

		mockMvc.perform(get("/games").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/games"))
				.andExpect(content().string(containsString("Pokémon")))
				.andExpect(content().string(containsString("/games/1")))
				.andExpect(content().string(containsString("Trang sau")));
	}

	@Test
	void selectingGameRendersSeriesAndBreadcrumb() throws Exception {
		GameResponse game = new GameResponse(1L, "Pokémon", "PKM", null, null, true, null);
		GameSeriesResponse series = new GameSeriesResponse(
				2L, 1L, "Pokémon", "Prismatic Horizons", "PH", LocalDate.of(2026, 1, 1), true,
				"https://res.cloudinary.com/demo/series-cover.jpg");
		when(publicCatalogService.getActiveGame(1L)).thenReturn(game);
		when(publicCatalogService.getActiveSeries(eq(1L), any(Pageable.class)))
				.thenReturn(page(List.of(series), 0, 1));

		mockMvc.perform(get("/games/1").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/game-series"))
				.andExpect(content().string(containsString("Prismatic Horizons")))
				.andExpect(content().string(containsString("https://res.cloudinary.com/demo/series-cover.jpg")))
				.andExpect(content().string(containsString("/series/2")))
				.andExpect(content().string(containsString("Pokémon")));
	}

	@Test
	void selectingSeriesRendersProductPageAndBreadcrumb() throws Exception {
		GameResponse game = new GameResponse(1L, "Pokémon", "PKM", null, null, true, null);
		SeriesResponse series = new SeriesResponse(
				2L, 1L, "Pokémon", "Prismatic Horizons", "PH", LocalDate.of(2026, 1, 1), true);
		PublicCatalogProductResponse product = new PublicCatalogProductResponse(
				3L, 4L, "Card Shop", "Radiant Card", "Radiant Card", "PH-01",
				ProductType.SINGLE, CardCondition.NEAR_MINT, new BigDecimal("25000"), 5, 12, null);
		when(publicCatalogService.getActiveSeriesProducts(eq(2L), any(Pageable.class)))
				.thenReturn(new PublicSeriesPageResponse(game, series, page(List.of(product), 0, 2)));

		mockMvc.perform(get("/series/2").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/series-products"))
				.andExpect(content().string(containsString("Radiant Card")))
				.andExpect(content().string(containsString("25.000 ₫")))
				.andExpect(content().string(containsString("/games/1")))
				.andExpect(content().string(containsString("Trang sau")));
	}

	private <T> PageResponse<T> page(List<T> content, int page, int totalPages) {
		return new PageResponse<>(content, page, 12, (long) totalPages * 12, totalPages,
				page == 0, page == totalPages - 1);
	}
}
