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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipperDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperDashboardMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void shipperDashboardRendersItsRoleSidebar() throws Exception {
		mockMvc.perform(get("/shipper/dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/demo-shipper"))
				.andExpect(content().string(containsString("Điều hướng Shipper")))
				.andExpect(content().string(containsString("Dashboard của Shipper")))
				.andExpect(content().string(containsString("href=\"/shipper/dashboard\"")));
	}
}
