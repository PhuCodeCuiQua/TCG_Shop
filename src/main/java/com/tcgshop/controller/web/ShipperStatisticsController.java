package com.tcgshop.controller.web;

import com.tcgshop.dto.response.ShipperStatisticsResponse;
import com.tcgshop.service.ShipperStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/shipper/statistics")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperStatisticsController {
	private final ShipperStatisticsService shipperStatisticsService;

	@GetMapping
	public String statistics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			Model model,
			HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
		ShipperStatisticsResponse statistics = shipperStatisticsService.getStatistics(from, to);
		model.addAttribute("statistics", statistics);
		model.addAttribute("dashboardRole", "SHIPPER");
		model.addAttribute("dashboardTitle", "Thống kê giao hàng");
		model.addAttribute("currentPath", "/shipper/statistics");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard Shipper", "url", "/shipper/dashboard"),
				Map.of("label", "Thống kê giao hàng", "url", "/shipper/statistics")));
		return "shipper/statistics";
	}
}
