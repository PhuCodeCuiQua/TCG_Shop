package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorSingleProductRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.VendorSingleProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorSingleProductController {

	private static final int PAGE_SIZE = 12;
	private static final String PAGE_URL = "/vendor/products/new/single";
	private static final String SEARCH_VIEW = "vendor/product-single-search";
	private static final String FORM_VIEW = "vendor/product-single-form";

	private final VendorSingleProductService vendorSingleProductService;

	@GetMapping(PAGE_URL)
	public String showPage(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long seriesId,
			@RequestParam(required = false) Long cardId,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		if (cardId != null) {
			CardListingResponse selectedCard = vendorSingleProductService.getListingCard(cardId);
			VendorSingleProductRequest request = new VendorSingleProductRequest();
			request.setCardId(selectedCard.id());
			request.setLanguage(selectedCard.language());
			prepareForm(model, request, selectedCard);
			return FORM_VIEW;
		}

		Page<CardListingResponse> cardsPage = vendorSingleProductService.searchCards(
				keyword, seriesId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
		model.addAttribute("cardsPage", cardsPage);
		model.addAttribute("seriesOptions", vendorSingleProductService.getActiveSeriesOptions());
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedSeriesId", seriesId);
		addDashboardModel(model);
		return SEARCH_VIEW;
	}

	@PostMapping(PAGE_URL)
	public String create(
			@Valid @ModelAttribute("singleProductRequest") VendorSingleProductRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (request.getCardId() == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn lại thẻ trước khi đăng bán.");
			return "redirect:" + PAGE_URL;
		}
		CardListingResponse selectedCard = vendorSingleProductService.getListingCard(request.getCardId());
		if (bindingResult.hasErrors()) {
			prepareForm(model, request, selectedCard);
			return FORM_VIEW;
		}

		try {
			vendorSingleProductService.createListing(request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã đăng bán thẻ thành công");
			return "redirect:/vendor/products";
		} catch (ConflictException exception) {
			model.addAttribute("submissionError", exception.getMessage());
			prepareForm(model, request, selectedCard);
			return FORM_VIEW;
		}
	}

	private void prepareForm(Model model, VendorSingleProductRequest request, CardListingResponse selectedCard) {
		model.addAttribute("singleProductRequest", request);
		model.addAttribute("selectedCard", selectedCard);
		model.addAttribute("seriesOptions", vendorSingleProductService.getActiveSeriesOptions());
		model.addAttribute("cardConditions", CardCondition.values());
		model.addAttribute("cardLanguages", CardLanguage.values());
		addDashboardModel(model);
	}

	private void addDashboardModel(Model model) {
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Đăng bán thẻ đơn");
		model.addAttribute("currentPath", "/vendor/products");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Sản phẩm", "url", "/vendor/products"),
				Map.of("label", "Đăng bán thẻ đơn", "url", PAGE_URL)));
	}
}
