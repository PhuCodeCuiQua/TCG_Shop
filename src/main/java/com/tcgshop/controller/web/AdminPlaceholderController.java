package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPlaceholderController {

	@GetMapping("/manager/orders")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public String orders(Authentication authentication, Model model) {
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		addLayoutModel(model, isAdmin ? "ADMIN" : "MANAGER", "Quản lý đơn hàng",
				"/manager/orders");
		return "manager/orders/list";
	}

	@GetMapping("/admin/roles")
	@PreAuthorize("hasRole('ADMIN')")
	public String roles(Model model) {
		addLayoutModel(model, "ADMIN", "Phân quyền", "/admin/roles");
		return "admin/roles/index";
	}

	@GetMapping("/admin/logs")
	@PreAuthorize("hasRole('ADMIN')")
	public String logs(Model model) {
		addLayoutModel(model, "ADMIN", "Log hệ thống", "/admin/logs");
		return "admin/logs/index";
	}

	private void addLayoutModel(Model model, String role, String title, String currentPath) {
		String dashboardPath = "ADMIN".equals(role) ? "/admin/dashboard" : "/manager/dashboard";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("dashboardHomeUrl", dashboardPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard", "url", dashboardPath),
				Map.of("label", title, "url", currentPath)));
	}
}
