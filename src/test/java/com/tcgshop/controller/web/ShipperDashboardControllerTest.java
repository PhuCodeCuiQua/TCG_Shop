package com.tcgshop.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class ShipperDashboardControllerTest {
	@Test
	void dashboardSelectsShipperLayoutAndSidebar() {
		ExtendedModelMap model = new ExtendedModelMap();

		assertEquals("dashboard/demo-shipper", new ShipperDashboardController().dashboard(model));
		assertEquals("SHIPPER", model.getAttribute("dashboardRole"));
		assertEquals("/shipper/dashboard", model.getAttribute("currentPath"));
		assertEquals("Dashboard Shipper", model.getAttribute("dashboardTitle"));
		assertEquals(List.of(
				Map.of("label", "Dashboard", "url", "/shipper/dashboard"),
				Map.of("label", "SHIPPER", "url", "/shipper/dashboard")),
				model.getAttribute("breadcrumbItems"));
	}
}
