package com.tcgshop.controller.web;

import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.service.PublicShopService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/shops")
@RequiredArgsConstructor
public class PublicShopController {
	private static final int PAGE_SIZE = 12;
	private static final String VIEW = "shop/public-shop";

	private final PublicShopService publicShopService;

	@GetMapping("/{id}")
	public String showShop(
			@PathVariable Long id,
			@RequestParam(required = false) ProductType productType,
			@RequestParam(name = "type", required = false) ProductType legacyType,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long gameId,
			@RequestParam(required = false) Long seriesId,
			@RequestParam(required = false) Rarity rarity,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		ProductType selectedProductType = productType != null ? productType : legacyType;
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
				Sort.by(Sort.Direction.DESC, "createdAt"));
		PublicShopPageResponse shopPage = publicShopService.getApprovedShopPage(
				id, selectedProductType, keyword, gameId, seriesId, rarity, minPrice, maxPrice, pageable);
		model.addAttribute("shop", shopPage.shop());
		model.addAttribute("productsPage", shopPage.products());
		model.addAttribute("productTypes", Arrays.asList(ProductType.values()));
		model.addAttribute("rarities", Arrays.asList(Rarity.values()));
		model.addAttribute("filterOptions", publicShopService.getFilterOptions(gameId));
		model.addAttribute("selectedType", selectedProductType);
		model.addAttribute("keyword", keyword);
		model.addAttribute("gameId", gameId);
		model.addAttribute("seriesId", seriesId);
		model.addAttribute("selectedRarity", rarity);
		model.addAttribute("minPrice", minPrice);
		model.addAttribute("maxPrice", maxPrice);
		int ratingStars = 0;
		if (shopPage.shop().avgRating() != null
				&& shopPage.shop().ratingCount() != null
				&& shopPage.shop().ratingCount() > 0) {
			ratingStars = Math.max(0, Math.min(5,
					shopPage.shop().avgRating().setScale(0, RoundingMode.HALF_UP).intValue()));
		}
		model.addAttribute("ratingStars", ratingStars);
		return VIEW;
	}
}
