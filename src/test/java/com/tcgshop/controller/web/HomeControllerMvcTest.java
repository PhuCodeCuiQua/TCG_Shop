package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void homeRendersMarketplaceSectionsAndMockProducts() throws Exception {
		mockMvc.perform(get("/").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(content().string(containsString("Best Sellers")))
				.andExpect(content().string(containsString("Latest Sets")))
				.andExpect(content().string(containsString("Radiant Vanguard")))
				.andExpect(content().string(containsString("home-hero.svg")))
				.andExpect(content().string(containsString("cart-icon")))
				.andExpect(content().string(containsString("Help &amp; Contact")));
	}
}
