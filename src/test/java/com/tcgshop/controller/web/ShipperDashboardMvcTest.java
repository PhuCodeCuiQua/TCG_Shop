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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tcgshop.dto.response.ShipperDashboardResponse;
import com.tcgshop.service.ShipperDashboardService;
import java.math.BigDecimal;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ShipperDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperDashboardMvcTest {
	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private ShipperDashboardService shipperDashboardService;

	@Test
	@org.springframework.security.test.context.support.WithMockUser(username = "shipper", roles = "SHIPPER")
	void shipperDashboardRendersItsRoleSidebar() throws Exception {
		when(shipperDashboardService.getDashboard())
				.thenReturn(new ShipperDashboardResponse(0, 0, BigDecimal.ZERO));
		mockMvc.perform(get("/shipper/dashboard").requestAttr("_csrf",
				new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isOk())
				.andExpect(view().name("shipper/dashboard"))
				.andExpect(content().string(containsString("Điều hướng Shipper")))
				.andExpect(content().string(containsString("Dashboard của Shipper")))
				.andExpect(content().string(containsString("href=\"/shipper/dashboard\"")));
	}
}
