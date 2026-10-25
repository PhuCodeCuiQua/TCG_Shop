package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.ShipperDeliveryChartResponse;
import com.tcgshop.service.ShipperStatisticsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipper/statistics")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperStatisticsRestController {
	private final ShipperStatisticsService shipperStatisticsService;

	@GetMapping
	public ShipperDeliveryChartResponse getDailyDeliveries(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return shipperStatisticsService.getDailyDeliveries(from, to);
	}
}
