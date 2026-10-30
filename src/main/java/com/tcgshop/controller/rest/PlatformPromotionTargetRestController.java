package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.PromotionTargetOptionResponse;
import com.tcgshop.service.PlatformPromotionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlatformPromotionTargetRestController {

	private final PlatformPromotionService promotionService;

	@GetMapping("/api/series")
	public List<PromotionTargetOptionResponse> getSeriesOptions(
			@RequestParam(required = false) Long gameId) {
		return promotionService.getSeriesOptions(gameId);
	}

	@GetMapping("/api/products")
	public List<PromotionTargetOptionResponse> getProductOptions(
			@RequestParam(required = false) Long gameId,
			@RequestParam(required = false) Long seriesId,
			@RequestParam(required = false) Long shopId,
			@RequestParam(required = false) String keyword) {
		return promotionService.searchProductOptions(gameId, seriesId, shopId, keyword);
	}
}
