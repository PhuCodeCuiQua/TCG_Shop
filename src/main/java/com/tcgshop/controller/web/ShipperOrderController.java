package com.tcgshop.controller.web;

import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderListResponse;
import com.tcgshop.service.ShipperOrderService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
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
@RequestMapping("/shipper/orders")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperOrderController {
	private static final int PAGE_SIZE = 10;
	private final ShipperOrderService shipperOrderService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "pickup") String tab,
			@RequestParam(defaultValue = "0") int page,
			Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		ShipperOrderListResponse orderList = shipperOrderService.getOrders(tab,
				PageRequest.of(Math.max(0, page), PAGE_SIZE,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		model.addAttribute("orderList", orderList);
		var responsePage = orderList.orders();
		model.addAttribute("paginationPage", new PageImpl<>(
				responsePage.content(), PageRequest.of(responsePage.page(), responsePage.size()),
				responsePage.totalElements()));
		addLayoutModel(model, "Đơn được phân công", "/shipper/orders");
		return "shipper/orders";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		ShipperOrderDetailResponse order = shipperOrderService.getOrderDetail(id);
		model.addAttribute("order", order);
		addLayoutModel(model, "Chi tiết đơn hàng", "/shipper/orders/" + id);
		return "shipper/order-detail";
	}

	private void addLayoutModel(Model model, String title, String currentPath) {
		model.addAttribute("dashboardRole", "SHIPPER");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard Shipper", "url", "/shipper/dashboard"),
				Map.of("label", "Đơn được phân công", "url", "/shipper/orders"),
				Map.of("label", title, "url", currentPath)));
	}
}
