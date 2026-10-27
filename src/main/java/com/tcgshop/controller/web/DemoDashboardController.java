package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoDashboardController {

	@GetMapping("/demo/dashboard/vendor")
	public String showVendorDashboard() {
		return "redirect:/vendor/dashboard";
	}

	@GetMapping("/demo/dashboard/shipper")
	public String showShipperDashboard(Model model) {
		return "redirect:/shipper/dashboard";
	}

	@GetMapping("/demo/dashboard/manager")
	public String showManagerDashboard(Model model) {
		addDashboardModel(model, "MANAGER", "Dashboard Manager", "/manager/dashboard");
		return "dashboard/demo-manager";
	}

	@GetMapping("/demo/dashboard/admin")
	public String showAdminDashboard(Model model) {
		addDashboardModel(model, "ADMIN", "Dashboard Admin", "/admin/dashboard");
		return "dashboard/demo-admin";
	}

	private void addDashboardModel(Model model, String role, String title, String currentPath) {
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard", "url", currentPath),
				Map.of("label", role, "url", currentPath)));
	}
}
