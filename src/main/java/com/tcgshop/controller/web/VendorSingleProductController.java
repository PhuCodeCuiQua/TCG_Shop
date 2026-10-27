package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorCustomCardRequest;
import com.tcgshop.dto.request.VendorSingleProductRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.VendorSingleProductService;
import jakarta.servlet.http.HttpSession;
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
	private static final String CUSTOM_CARD_VIEW = "vendor/product-single-custom";
	private static final String PENDING_CARD_SESSION_KEY = "vendorPendingSingleCardId";

	private final VendorSingleProductService vendorSingleProductService;

	@GetMapping(PAGE_URL)
	public String showPage(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long seriesId,
			@RequestParam(required = false) Long cardId,
			@RequestParam(defaultValue = "0") int page,
			HttpSession session,
			Model model) {
		if (cardId != null) {
			boolean pendingCard = cardId.equals(session.getAttribute(PENDING_CARD_SESSION_KEY));
			CardListingResponse selectedCard = pendingCard
					? vendorSingleProductService.getPendingListingCard(cardId)
					: vendorSingleProductService.getListingCard(cardId);
			VendorSingleProductRequest request = new VendorSingleProductRequest();
			request.setCardId(selectedCard.id());
			request.setLanguage(selectedCard.language());
			prepareForm(model, request, selectedCard, pendingCard);
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

	@GetMapping(PAGE_URL + "/custom")
	public String showCustomCardForm(Model model) {
		prepareCustomCardForm(model, new VendorCustomCardRequest());
		return CUSTOM_CARD_VIEW;
	}

	@PostMapping(PAGE_URL + "/custom")
	public String createCustomCard(
			@Valid @ModelAttribute("customCardRequest") VendorCustomCardRequest request,
			BindingResult bindingResult,
			Model model,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (request.getImage() == null || request.getImage().isEmpty()) {
			bindingResult.rejectValue("image", "required", "Vui lòng tải ảnh thẻ lên");
		}
		if (bindingResult.hasErrors()) {
			prepareCustomCardForm(model, request);
			return CUSTOM_CARD_VIEW;
		}

		try {
			CardListingResponse card = vendorSingleProductService.createPendingCard(request);
			session.setAttribute(PENDING_CARD_SESSION_KEY, card.id());
			redirectAttributes.addFlashAttribute("successMessage",
					"Thẻ đã được gửi chờ duyệt. Hãy hoàn tất thông tin niêm yết của shop.");
			return "redirect:" + PAGE_URL + "?cardId=" + card.id();
		} catch (ConflictException | BadRequestException exception) {
			model.addAttribute("submissionError", exception.getMessage());
			prepareCustomCardForm(model, request);
			return CUSTOM_CARD_VIEW;
		}
	}

	@PostMapping(PAGE_URL)
	public String create(
			@Valid @ModelAttribute("singleProductRequest") VendorSingleProductRequest request,
			BindingResult bindingResult,
			Model model,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (request.getCardId() == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn lại thẻ trước khi đăng bán.");
			return "redirect:" + PAGE_URL;
		}
		boolean pendingCard = request.getCardId().equals(session.getAttribute(PENDING_CARD_SESSION_KEY));
		CardListingResponse selectedCard = pendingCard
				? vendorSingleProductService.getPendingListingCard(request.getCardId())
				: vendorSingleProductService.getListingCard(request.getCardId());
		if (bindingResult.hasErrors()) {
			prepareForm(model, request, selectedCard, pendingCard);
			return FORM_VIEW;
		}

		try {
			if (pendingCard) {
				vendorSingleProductService.createListingFromPendingCard(request);
			} else {
				vendorSingleProductService.createListing(request);
			}
			if (pendingCard) {
				session.removeAttribute(PENDING_CARD_SESSION_KEY);
			}
			redirectAttributes.addFlashAttribute("successMessage", pendingCard
					? "Đã tạo sản phẩm. Thẻ gốc đang chờ Admin/Manager duyệt nên chưa hiển thị công khai."
					: "Đã đăng bán thẻ thành công");
			return "redirect:/vendor/products";
		} catch (ConflictException exception) {
			model.addAttribute("submissionError", exception.getMessage());
			prepareForm(model, request, selectedCard, pendingCard);
			return FORM_VIEW;
		}
	}

	private void prepareForm(Model model, VendorSingleProductRequest request, CardListingResponse selectedCard,
			boolean pendingCard) {
		model.addAttribute("singleProductRequest", request);
		model.addAttribute("selectedCard", selectedCard);
		model.addAttribute("pendingCustomCard", pendingCard);
		model.addAttribute("seriesOptions", vendorSingleProductService.getActiveSeriesOptions());
		model.addAttribute("cardConditions", CardCondition.values());
		model.addAttribute("cardLanguages", CardLanguage.values());
		addDashboardModel(model);
	}

	private void prepareCustomCardForm(Model model, VendorCustomCardRequest request) {
		model.addAttribute("customCardRequest", request);
		model.addAttribute("seriesOptions", vendorSingleProductService.getActiveSeriesOptions());
		model.addAttribute("rarities", Rarity.values());
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
