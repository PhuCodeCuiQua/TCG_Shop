package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorOtherProductRequest;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.service.VendorOtherProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorOtherProductController {

	private static final String PAGE_URL = "/vendor/products/new/other";
	private static final String FORM_VIEW = "vendor/product-other-form";
	private static final List<ProductType> SELLABLE_TYPES = List.of(
			ProductType.BOOSTER_BOX,
			ProductType.BOOSTER_PACK,
			ProductType.BUNDLE,
			ProductType.SUPPLY);
	private static final List<Map<String, String>> SUPPLY_CATEGORIES = List.of(
			Map.of("value", "SLEEVES", "label", "Sleeves"),
			Map.of("value", "DECK_BOX", "label", "Deck Box"),
			Map.of("value", "PLAYMAT", "label", "Playmat"),
			Map.of("value", "OTHER", "label", "Phụ kiện khác"));

	private final VendorOtherProductService vendorOtherProductService;

	@GetMapping(PAGE_URL)
	public String showForm(Model model) {
		if (!model.containsAttribute("productRequest")) {
			model.addAttribute("productRequest", new VendorOtherProductRequest());
		}
		prepareModel(model);
		return FORM_VIEW;
	}

	@PostMapping(PAGE_URL)
	public String create(
			@Valid @ModelAttribute("productRequest") VendorOtherProductRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		validateConditionalFields(request, bindingResult);
		if (bindingResult.hasErrors()) {
			prepareModel(model);
			return FORM_VIEW;
		}

		try {
			vendorOtherProductService.createProduct(request);
			redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được đăng bán thành công.");
			return "redirect:/vendor/products";
		} catch (BadRequestException exception) {
			attachServiceError(request, bindingResult, exception);
			prepareModel(model);
			return FORM_VIEW;
		}
	}

	private void validateConditionalFields(VendorOtherProductRequest request, BindingResult bindingResult) {
		if (request.getType() == ProductType.BOOSTER_BOX
				|| request.getType() == ProductType.BOOSTER_PACK
				|| request.getType() == ProductType.BUNDLE) {
			if (request.getSeriesId() == null) {
				bindingResult.rejectValue("seriesId", "required", "Vui lòng chọn series");
			}
			if (request.getLanguage() == null) {
				bindingResult.rejectValue("language", "required", "Vui lòng chọn ngôn ngữ");
			}
		}
		if (request.getType() == ProductType.SUPPLY
				&& (request.getAccessoryCategory() == null || request.getAccessoryCategory().isBlank())) {
			bindingResult.rejectValue("accessoryCategory", "required", "Vui lòng chọn loại phụ kiện");
		}
	}

	private void attachServiceError(
			VendorOtherProductRequest request,
			BindingResult bindingResult,
			BadRequestException exception) {
		String message = exception.getMessage();
		if (request.getPrice() == null || request.getPrice().signum() <= 0) {
			bindingResult.rejectValue("price", "invalid", message);
		} else if (request.getStock() == null || request.getStock() <= 0) {
			bindingResult.rejectValue("stock", "invalid", message);
		} else if (request.getName() == null || request.getName().isBlank()) {
			bindingResult.rejectValue("name", "invalid", message);
		} else if (request.getDescription() == null || request.getDescription().isBlank()) {
			bindingResult.rejectValue("description", "invalid", message);
		} else if (request.getType() == ProductType.SUPPLY) {
			bindingResult.rejectValue("accessoryCategory", "invalid", message);
		} else if (request.getType() == ProductType.BOOSTER_BOX
				|| request.getType() == ProductType.BOOSTER_PACK
				|| request.getType() == ProductType.BUNDLE) {
			bindingResult.rejectValue("seriesId", "invalid", message);
		} else {
			bindingResult.reject("productRequest.invalid", message);
		}
	}

	private void prepareModel(Model model) {
		model.addAttribute("productTypes", SELLABLE_TYPES);
		model.addAttribute("cardLanguages", CardLanguage.values());
		model.addAttribute("supplyCategories", SUPPLY_CATEGORIES);
		model.addAttribute("seriesOptions", vendorOtherProductService.getActiveSeriesOptions());
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Đăng bán sản phẩm");
		model.addAttribute("currentPath", "/vendor/products");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Sản phẩm", "url", "/vendor/products"),
				Map.of("label", "Đăng bán sản phẩm", "url", PAGE_URL)));
	}
}
