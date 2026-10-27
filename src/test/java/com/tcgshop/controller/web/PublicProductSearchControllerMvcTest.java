package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.dto.response.PublicProductSearchOptionsResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.service.PublicProductSearchService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicProductSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class PublicProductSearchControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@MockBean private PublicProductSearchService publicProductSearchService;

	@Test
	void filtersAndRankingTabsPreserveSearchParametersAndRenderGroupedSingles() throws Exception {
		PublicProductSearchItemResponse item = new PublicProductSearchItemResponse(
				9L, 27L, 9L, true, null, null, "Radiant Vanguard", "PH-001",
				"Pokémon", "Prismatic Horizons", ProductType.SINGLE, CardLanguage.ENGLISH,
				null, new BigDecimal("20000"), 3L, 11L, new BigDecimal("4.80"),
				12L, 16L, null);
		when(publicProductSearchService.getFilterOptions(1L))
				.thenReturn(new PublicProductSearchOptionsResponse(
						List.of(new GameResponse(1L, "Pokémon", "PKM", null, null, true, null)),
						List.of(new SeriesResponse(2L, 1L, "Pokémon", "Prismatic Horizons", "PH",
								LocalDate.of(2026, 1, 1), true))));
		when(publicProductSearchService.search(any(PublicProductSearchRequest.class)))
				.thenReturn(page(List.of(item), 1, 2));

		mockMvc.perform(get("/products")
				.param("keyword", "PH-001")
				.param("gameId", "1")
				.param("seriesId", "2")
				.param("type", "SINGLE")
				.param("language", "ENGLISH")
				.param("priceFrom", "10000")
				.param("priceTo", "30000")
				.param("sort", "NEWEST")
				.param("page", "1")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/product-search"))
				.andExpect(content().string(containsString("Radiant Vanguard")))
				.andExpect(content().string(containsString("data-product-id=\"27\"")))
				.andExpect(content().string(containsString("3 người bán")))
				.andExpect(content().string(containsString("gameId=1")))
				.andExpect(content().string(containsString("seriesId=2")))
				.andExpect(content().string(containsString("priceFrom=10000")))
				.andExpect(content().string(containsString("sort=NEWEST")))
				.andExpect(content().string(containsString("page=1")));
	}

	@Test
	void topRatedTabDisplaysItsTopTwentyHeading() throws Exception {
		when(publicProductSearchService.getFilterOptions(null))
				.thenReturn(new PublicProductSearchOptionsResponse(List.of(), List.of()));
		when(publicProductSearchService.search(any(PublicProductSearchRequest.class)))
				.thenReturn(page(List.of(), 0, 1));

		mockMvc.perform(get("/products")
				.param("tab", "TOP_RATED")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Top 20 · Đánh giá cao")));
	}

	@Test
	void supplyWithoutAnImageRendersItsAccessoryPlaceholderAndErrorFallback() throws Exception {
		PublicProductSearchItemResponse supply = new PublicProductSearchItemResponse(
				7L, 7L, null, false, 2L, "Shop", "Sleeves", null, null, null,
				ProductType.SUPPLY, null, null, new BigDecimal("50000"),
				1L, 0L, null, 0L, 0L, null);
		when(publicProductSearchService.getFilterOptions(null))
				.thenReturn(new PublicProductSearchOptionsResponse(List.of(), List.of()));
		when(publicProductSearchService.search(any(PublicProductSearchRequest.class)))
				.thenReturn(page(List.of(supply), 0, 1));

		mockMvc.perform(get("/products")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("src=\"/img/placeholder-supply.svg\"")))
				.andExpect(content().string(containsString(
						"onerror=\"this.onerror=null;this.src='/img/placeholder-supply.svg';\"")));
	}

	@Test
	void reversedPriceRangeShowsValidationErrorWithoutSearching() throws Exception {
		when(publicProductSearchService.getFilterOptions(null))
				.thenReturn(new PublicProductSearchOptionsResponse(List.of(), List.of()));

		mockMvc.perform(get("/products")
				.param("priceFrom", "30000")
				.param("priceTo", "10000")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Giá từ phải nhỏ hơn hoặc bằng giá đến")));

		verify(publicProductSearchService, never()).search(any(PublicProductSearchRequest.class));
	}

	@Test
	void negativePriceIsRejectedByValidation() throws Exception {
		when(publicProductSearchService.getFilterOptions(null))
				.thenReturn(new PublicProductSearchOptionsResponse(List.of(), List.of()));

		mockMvc.perform(get("/products")
				.param("priceFrom", "-1")
				.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Giá từ không được âm")));

		verify(publicProductSearchService, never()).search(any(PublicProductSearchRequest.class));
	}

	private PageResponse<PublicProductSearchItemResponse> page(
			List<PublicProductSearchItemResponse> content, int page, int totalPages) {
		return new PageResponse<>(content, page, 20, (long) totalPages * 20, totalPages,
				page == 0, page == totalPages - 1);
	}
}
