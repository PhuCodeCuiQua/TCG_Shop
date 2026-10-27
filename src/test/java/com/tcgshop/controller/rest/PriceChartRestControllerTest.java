package com.tcgshop.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.PriceChartPointResponse;
import com.tcgshop.dto.response.PriceChartResponse;
import com.tcgshop.service.PriceChartService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PriceChartRestControllerTest {
	private final PriceChartService service = mock(PriceChartService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new PriceChartRestController(service)).build();
	}

	@Test
	void returnsChartJsArraysAlongsideExistingPointsContract() throws Exception {
		var firstDate = LocalDate.of(2026, 10, 26);
		var secondDate = LocalDate.of(2026, 10, 27);
		var response = new PriceChartResponse(42L, 30, firstDate, secondDate, List.of(
				new PriceChartPointResponse(firstDate, new BigDecimal("120000"), new BigDecimal("118000")),
				new PriceChartPointResponse(secondDate, new BigDecimal("125000"), new BigDecimal("121000"))),
				new BigDecimal("125000"), null, null, false);
		when(service.getChart(42L, 30)).thenReturn(response);

		mockMvc.perform(get("/api/cards/42/price-history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.labels[0]").value("2026-10-26"))
				.andExpect(jsonPath("$.labels[1]").value("2026-10-27"))
				.andExpect(jsonPath("$.data[0]").value(120000))
				.andExpect(jsonPath("$.data[1]").value(125000))
				.andExpect(jsonPath("$.averageData[0]").value(118000))
				.andExpect(jsonPath("$.averageData[1]").value(121000))
				.andExpect(jsonPath("$.points[0].date").value("2026-10-26"))
				.andExpect(jsonPath("$.points[0].marketPrice").value(120000));

		verify(service).getChart(42L, 30);
	}

	@Test
	void returnsEmptyChartArraysWhenThereIsNoHistory() throws Exception {
		when(service.getChart(42L, 7)).thenReturn(
				new PriceChartResponse(42L, 7, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 27),
						List.of(), null, null, null, true));

		mockMvc.perform(get("/api/cards/42/price-history").param("range", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.labels").isEmpty())
				.andExpect(jsonPath("$.data").isEmpty())
				.andExpect(jsonPath("$.averageData").isEmpty())
				.andExpect(jsonPath("$.points").isEmpty());

		verify(service).getChart(42L, 7);
	}
}
