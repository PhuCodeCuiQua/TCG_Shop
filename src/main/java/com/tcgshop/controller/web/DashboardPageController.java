package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardPageController {

	@GetMapping("/admin/dashboard")
	public String adminDashboard(Model model) {
		model.addAttribute("apiBase", "/api/admin/dashboard");
		model.addAttribute("pageTitle", "Dashboard toàn hệ thống");
		addLayoutModel(model, "ADMIN", "Dashboard toàn hệ thống", "/admin/dashboard");
		return "manager/dashboard-stats";
	}

	@GetMapping("/manager/dashboard")
	public String managerDashboard(Model model) {
		model.addAttribute("apiBase", "/api/manager/dashboard");
		model.addAttribute("pageTitle", "Dashboard quản lý");
		addLayoutModel(model, "MANAGER", "Dashboard quản lý", "/manager/dashboard");
		return "manager/dashboard-stats";
	}

	private void addLayoutModel(Model model, String role, String title, String path) {
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", path);
		model.addAttribute("breadcrumbItems", List.of(Map.of("label", title, "url", path)));
	}
}