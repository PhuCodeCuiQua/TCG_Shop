package com.tcgshop.controller.web;

import com.tcgshop.entity.enums.RoleName;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

	@GetMapping("/manager/reports")
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public String managerReports(Model model, Authentication authentication) {
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> RoleName.ROLE_ADMIN.name().equals(authority.getAuthority()));
		String role = isAdmin ? "ADMIN" : "MANAGER";
		String title = "Báo cáo hệ thống";

		model.addAttribute("apiBase", isAdmin ? "/api/admin/dashboard" : "/api/manager/dashboard");
		model.addAttribute("pageTitle", title);
		addLayoutModel(model, role, title, "/manager/reports");
		return "manager/dashboard-stats";
	}

	private void addLayoutModel(Model model, String role, String title, String path) {
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", path);
		model.addAttribute("breadcrumbItems", List.of(Map.of("label", title, "url", path)));
	}
}