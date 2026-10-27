package com.tcgshop.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcgshop.dto.response.ShipperDashboardResponse;
import com.tcgshop.service.ShipperDashboardService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.springframework.ui.ExtendedModelMap;

class ShipperDashboardControllerTest {
	@Test
	void dashboardSelectsShipperLayoutAndSidebar() {
		ExtendedModelMap model = new ExtendedModelMap();
		ShipperDashboardService service = Mockito.mock(ShipperDashboardService.class);
		when(service.getDashboard()).thenReturn(new ShipperDashboardResponse(3, 2, new BigDecimal("125000")));

		assertEquals("shipper/dashboard", new ShipperDashboardController(service).dashboard(model));
		assertEquals("SHIPPER", model.getAttribute("dashboardRole"));
		assertEquals("/shipper/dashboard", model.getAttribute("currentPath"));
		assertEquals("Dashboard Shipper", model.getAttribute("dashboardTitle"));
		assertEquals(List.of(
				Map.of("label", "Dashboard", "url", "/shipper/dashboard"),
				Map.of("label", "SHIPPER", "url", "/shipper/dashboard")),
				model.getAttribute("breadcrumbItems"));
		assertEquals(new ShipperDashboardResponse(3, 2, new BigDecimal("125000")), model.getAttribute("statistics"));
	}
}
