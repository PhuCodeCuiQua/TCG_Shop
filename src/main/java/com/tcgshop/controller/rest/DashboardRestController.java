package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.DashboardSummaryResponse;
import com.tcgshop.dto.response.OrderStatusCountResponse;
import com.tcgshop.dto.response.RevenuePointResponse;
import com.tcgshop.dto.response.TopProductResponse;
import com.tcgshop.dto.response.TopShopResponse;
import com.tcgshop.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/manager/** cho Manager và Admin, /api/admin/** chỉ Admin (SecurityConfig).
 * Hoa hồng chỉ có trong dữ liệu khi người gọi là Admin (xử lý trong service).
 */
@RestController
@RequestMapping({ "/api/manager/dashboard", "/api/admin/dashboard" })
public class DashboardRestController {
	private final DashboardService service;

	public DashboardRestController(DashboardService service) {
		this.service = service;
	}

	@GetMapping("/summary")
	public DashboardSummaryResponse summary(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return service.getSummary(from, to);
	}

	@GetMapping("/revenue")
	public List<RevenuePointResponse> revenue(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "day") String group) {
		return service.getRevenueSeries(from, to, group);
	}

	@GetMapping("/orders-by-status")
	public List<OrderStatusCountResponse> ordersByStatus(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return service.getOrdersByStatus(from, to);
	}

	@GetMapping("/top-shops")
	public List<TopShopResponse> topShops(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "5") int limit) {
		return service.getTopShops(from, to, limit);
	}

	@GetMapping("/top-products")
	public List<TopProductResponse> topProducts(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "5") int limit) {
		return service.getTopProducts(from, to, limit);
	}
}