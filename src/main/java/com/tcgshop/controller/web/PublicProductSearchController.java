package com.tcgshop.controller.web;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.request.PublicProductSearchRequest.SortBy;
import com.tcgshop.dto.request.PublicProductSearchRequest.TopTab;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.dto.response.PublicProductSearchOptionsResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.service.PublicProductSearchService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
public class PublicProductSearchController {

	private static final int PAGE_SIZE = 20;
	private final PublicProductSearchService publicProductSearchService;

	@GetMapping("/products")
	public String search(
			@Valid @ModelAttribute("filters") PublicProductSearchRequest filters,
			BindingResult bindingResult,
			Model model) {
		PublicProductSearchOptionsResponse filterOptions =
				publicProductSearchService.getFilterOptions(filters.getGameId());
		PageResponse<PublicProductSearchItemResponse> results = bindingResult.hasErrors()
				? emptyPage(filters.getPage())
				: publicProductSearchService.search(filters);

		model.addAttribute("results", results);
		model.addAttribute("filterOptions", filterOptions);
		model.addAttribute("sortOptions", List.of(
				SortBy.PRICE_ASC, SortBy.PRICE_DESC, SortBy.NEWEST, SortBy.BEST_SELLING, SortBy.TOP_RATED));
		model.addAttribute("topTabs", Arrays.asList(TopTab.values()));
		model.addAttribute("productTypes", Arrays.asList(ProductType.values()));
		model.addAttribute("rarities", Arrays.asList(Rarity.values()));
		model.addAttribute("languages", Arrays.asList(CardLanguage.values()));
		model.addAttribute("conditions", Arrays.asList(CardCondition.values()));
		model.addAttribute("filterErrors", bindingResult.getAllErrors());
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Trang chủ", "url", "/"),
				Map.of("label", "Tìm kiếm sản phẩm", "url", "/products")));
		return "guest/product-search";
	}

	private PageResponse<PublicProductSearchItemResponse> emptyPage(int page) {
		return PageResponse.from(new PageImpl<>(List.of(), PageRequest.of(Math.max(page, 0), PAGE_SIZE), 0));
	}
}
