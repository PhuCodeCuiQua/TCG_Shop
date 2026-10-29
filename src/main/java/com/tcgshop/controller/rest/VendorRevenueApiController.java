package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.VendorRevenueChartResponse;
import com.tcgshop.service.VendorRevenueService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/revenue")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorRevenueApiController {
	private final VendorRevenueService vendorRevenueService;

	@GetMapping
	public VendorRevenueChartResponse getRevenueCharts(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "day") String groupBy) {
		return vendorRevenueService.getRevenueCharts(from, to, groupBy);
	}
}
