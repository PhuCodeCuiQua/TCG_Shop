package com.tcgshop.controller.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.response.ShipperStatisticsResponse;
import com.tcgshop.service.ShipperStatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipperStatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperStatisticsControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private ShipperStatisticsService shipperStatisticsService;

	@Test
	void statisticsPageRendersDateFilterCardsAndChartPanel() throws Exception {
		when(shipperStatisticsService.getStatistics(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25)))
				.thenReturn(new ShipperStatisticsResponse(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25),
						4, 1, 2, new BigDecimal("80.00"), new BigDecimal("350000")));

		mockMvc.perform(get("/shipper/statistics")
					.param("from", "2026-10-01")
					.param("to", "2026-10-25")
					.requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("shipper/statistics"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tỉ lệ thành công")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("COD đã thu")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("shipperDeliveriesChart")));
	}
}
