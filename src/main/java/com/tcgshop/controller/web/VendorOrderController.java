package com.tcgshop.controller.web;

import com.tcgshop.dto.response.VendorOrderDetailResponse;
import com.tcgshop.dto.response.VendorOrderListResponse;
import com.tcgshop.service.VendorOrderService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/vendor/orders")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorOrderController {
	private static final int PAGE_SIZE = 10;
	private final VendorOrderService vendorOrderService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "new") String tab,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		VendorOrderListResponse orderList = vendorOrderService.getOrders(tab, q,
				PageRequest.of(Math.max(0, page), PAGE_SIZE,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		model.addAttribute("orderList", orderList);
		addLayoutModel(model, "Đơn hàng", "/vendor/orders");
		return "vendor/orders";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		VendorOrderDetailResponse order = vendorOrderService.getOrderDetail(id);
		model.addAttribute("order", order);
		addLayoutModel(model, "Chi tiết đơn hàng", "/vendor/orders");
		return "vendor/order-detail";
	}

	private void addLayoutModel(Model model, String title, String currentPath) {
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Đơn hàng", "url", "/vendor/orders"),
				Map.of("label", title, "url", currentPath)));
	}
}
