package com.tcgshop.controller.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcgshop.dto.response.ShipperDeliveryChartResponse;
import com.tcgshop.service.ShipperStatisticsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipperStatisticsRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperStatisticsRestControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private ShipperStatisticsService shipperStatisticsService;

	@Test
	void chartApiReturnsDailyCountsAsJson() throws Exception {
		when(shipperStatisticsService.getDailyDeliveries(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)))
				.thenReturn(new ShipperDeliveryChartResponse(List.of("2026-10-01", "2026-10-02"), List.of(2L, 1L)));

		mockMvc.perform(get("/api/shipper/statistics")
					.param("from", "2026-10-01")
					.param("to", "2026-10-02"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(content().json("""
					{"labels":["2026-10-01","2026-10-02"],"orderCounts":[2,1]}
					"""));
	}
}
