package com.tcgshop.controller.web;

import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.service.ManagerOrderService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/manager/orders")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerOrderController {
	private static final int PAGE_SIZE = 12;
	private final ManagerOrderService managerOrderService;

	@GetMapping
	public String list(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(defaultValue = "0") int page,
			Authentication authentication, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		var orderPage = managerOrderService.searchOrders(keyword, status,
				PageRequest.of(Math.max(0, page), PAGE_SIZE,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		model.addAttribute("orders", orderPage);
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("statuses", OrderStatus.values());
		addLayoutModel(model, authentication, "Quản lý đơn hàng", "/manager/orders");
		return "manager/orders/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Authentication authentication,
			Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		model.addAttribute("order", managerOrderService.getOrderDetail(id));
		addLayoutModel(model, authentication, "Chi tiết đơn hàng", "/manager/orders");
		return "manager/orders/detail";
	}

	private void addLayoutModel(Model model, Authentication authentication, String title, String currentPath) {
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		String role = isAdmin ? "ADMIN" : "MANAGER";
		String dashboardPath = isAdmin ? "/admin/dashboard" : "/manager/dashboard";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("dashboardHomeUrl", dashboardPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard", "url", dashboardPath),
				Map.of("label", "Quản lý đơn hàng", "url", "/manager/orders"),
				Map.of("label", title, "url", currentPath)));
	}
}
