package com.tcgshop.controller.web;

import com.tcgshop.service.VendorRevenueService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/vendor/revenue")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorRevenueController {
	private static final int PAGE_SIZE = 10;
	private final VendorRevenueService vendorRevenueService;

	@GetMapping
	public String revenue(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		model.addAttribute("report", vendorRevenueService.getRevenueReport(from, to,
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.unsorted())));
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Doanh thu shop");
		model.addAttribute("currentPath", "/vendor/revenue");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Kênh người bán", "url", "/vendor/dashboard"),
				Map.of("label", "Doanh thu", "url", "/vendor/revenue")));
		return "vendor/revenue";
	}
}
