package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.response.HomeGameResponse;
import com.tcgshop.dto.response.HomePageResponse;
import com.tcgshop.dto.response.HomeProductResponse;
import com.tcgshop.service.HomePageService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private HomePageService homePageService;

	@Test
	void homeRendersMarketplaceSectionsAndDatabaseProducts() throws Exception {
		HomeProductResponse product = new HomeProductResponse(
				12L, 3L, "Radiant Vanguard", "Radiance", "Riftbound",
				new BigDecimal("1250000.00"), "/img/home-card-riftbound.svg", 24);
		when(homePageService.getHomePage()).thenReturn(new HomePageResponse(
				List.of(new HomeGameResponse(7L, "Riftbound", "R", null)),
				List.of(product), List.of(), List.of()));

		mockMvc.perform(get("/").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(content().string(containsString("Best Sellers")))
				.andExpect(content().string(containsString("BÁN CHẠY")))
				.andExpect(content().string(containsString("/games/7")))
				.andExpect(content().string(containsString("Latest Sets")))
				.andExpect(content().string(containsString("Radiant Vanguard")))
				.andExpect(content().string(containsString("Riftbound")))
				.andExpect(content().string(containsString("1.250.000 ₫")))
				.andExpect(content().string(containsString("home-hero.svg")))
				.andExpect(content().string(containsString("cart-icon")))
				.andExpect(content().string(containsString("Help &amp; Contact")));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userHomeRendersRecentlyViewedAndRecommendationSections() throws Exception {
		HomeProductResponse product = new HomeProductResponse(
				21L, 3L, "Collector Card", "First Series", "TCG", new BigDecimal("2500.00"), null, 15);
		when(homePageService.getHomePage()).thenReturn(new HomePageResponse(
				List.of(), List.of(), List.of(product), List.of(product)));

		mockMvc.perform(get("/").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Xem gần đây")))
				.andExpect(content().string(containsString("Gợi ý dành cho bạn")))
				.andExpect(content().string(containsString("Collector Card")));
	}
}
