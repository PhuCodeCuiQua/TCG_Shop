package com.tcgshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardPageController {

	@GetMapping("/admin/dashboard")
	public String adminDashboard(Model model) {
		model.addAttribute("apiBase", "/api/admin/dashboard");
		model.addAttribute("pageTitle", "Dashboard toàn hệ thống");
		return "manager/dashboard-stats";
	}

	@GetMapping("/manager/dashboard")
	public String managerDashboard(Model model) {
		model.addAttribute("apiBase", "/api/manager/dashboard");
		model.addAttribute("pageTitle", "Dashboard quản lý");
		return "manager/dashboard-stats";
	}
}