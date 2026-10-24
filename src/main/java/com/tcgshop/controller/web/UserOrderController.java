package com.tcgshop.controller.web;

import com.tcgshop.service.UserOrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserOrderController {
	private final UserOrderService userOrderService;

	public UserOrderController(UserOrderService userOrderService) {
		this.userOrderService = userOrderService;
	}

	@GetMapping("/orders")
	public String list(@RequestParam(defaultValue = "all") String tab,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		model.addAttribute("orderList", userOrderService.listMyOrders(tab, q, page));
		return "user/orders/index";
	}

	@GetMapping("/orders/{id}")
	public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		model.addAttribute("order", userOrderService.getMyOrder(id));
		return "user/orders/detail";
	}
}
