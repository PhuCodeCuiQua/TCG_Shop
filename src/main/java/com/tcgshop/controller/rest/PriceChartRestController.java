package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.PriceChartResponse;
import com.tcgshop.service.PriceChartService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/cards/** là công khai (SECURITY_SPEC mục 2). */
@RestController
@RequestMapping("/api/cards")
public class PriceChartRestController {
	private final PriceChartService service;

	public PriceChartRestController(PriceChartService service) {
		this.service = service;
	}

	@GetMapping(value = "/{id}/price-history", produces = MediaType.APPLICATION_JSON_VALUE)
	public PriceChartResponse priceHistory(@PathVariable Long id,
			@RequestParam(defaultValue = "30") int range) {
		return service.getChart(id, range);
	}
}