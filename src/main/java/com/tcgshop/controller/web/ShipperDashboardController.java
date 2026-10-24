package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasRole('SHIPPER')")
public class ShipperDashboardController {
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		String currentPath = "/shipper/dashboard";
		model.addAttribute("dashboardRole", "SHIPPER");
		model.addAttribute("dashboardTitle", "Dashboard Shipper");
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard", "url", currentPath),
				Map.of("label", "SHIPPER", "url", currentPath)));
		return "dashboard/demo-shipper";
	}
}
