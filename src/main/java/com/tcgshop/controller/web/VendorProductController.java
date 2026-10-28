package com.tcgshop.controller.web;

import com.tcgshop.dto.response.VendorProductResponse;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.service.VendorProductService;
import com.tcgshop.service.PromotionService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorProductController {

	private static final int PAGE_SIZE = 12;
	private static final String BASE_URL = "/vendor/products";

	private final VendorProductService vendorProductService;
	private final PromotionService promotionService;

	@GetMapping(BASE_URL)
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) ProductType type,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Page<VendorProductResponse> productsPage = vendorProductService.searchCurrentVendorProducts(
				keyword, type, status,
				PageRequest.of(Math.max(0, page), PAGE_SIZE));
		model.addAttribute("effectivePrices", promotionService.getEffectivePrices(
				productsPage.getContent().stream().map(VendorProductResponse::id).toList()));

		model.addAttribute("productsPage", productsPage);
		model.addAttribute("productTypes", ProductType.values());
		model.addAttribute("productStatuses", ProductStatus.values());
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedType", type);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Sản phẩm của Chi nhánh");
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Sản phẩm", "url", BASE_URL)));
		return "vendor/products";
	}
}
