package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorPromotionRequest;
import com.tcgshop.dto.response.VendorPromotionResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.service.VendorPromotionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendor/promotions")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorPromotionController {
	private static final int PAGE_SIZE = 10;
	private static final String LIST_VIEW = "vendor/promotions";
	private static final String FORM_VIEW = "vendor/promotion-form";

	private final VendorPromotionService promotionService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		Page<VendorPromotionResponse> promotions = promotionService.getCurrentShopPromotions(
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
		model.addAttribute("promotionsPage", promotions);
		prepareLayout(model, "Chương trình khuyến mãi", "/vendor/promotions");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("promotionRequest", new VendorPromotionRequest());
		prepareForm(model, null);
		return FORM_VIEW;
	}

	@PostMapping("/new")
	public String create(@Valid @ModelAttribute("promotionRequest") VendorPromotionRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		return save(null, request, bindingResult, model, redirectAttributes);
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		VendorPromotionResponse promotion = promotionService.getCurrentShopPromotion(id);
		if (!model.containsAttribute("promotionRequest")) {
			VendorPromotionRequest request = new VendorPromotionRequest();
			request.setTitle(promotion.title());
			request.setDiscountPercent(promotion.discountPercent());
			request.setStartDate(promotion.startDate());
			request.setEndDate(promotion.endDate());
			request.setProductIds(promotion.productIds());
			model.addAttribute("promotionRequest", request);
		}
		model.addAttribute("promotion", promotion);
		prepareForm(model, id);
		return FORM_VIEW;
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("promotionRequest") VendorPromotionRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		return save(id, request, bindingResult, model, redirectAttributes);
	}

	@PostMapping("/{id}/toggle-status")
	public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		VendorPromotionResponse updated = promotionService.toggleCurrentShopPromotion(id);
		redirectAttributes.addFlashAttribute("successMessage",
				updated.status() == ActiveStatus.ACTIVE ? "Đã bật chương trình khuyến mãi" : "Đã tạm dừng chương trình khuyến mãi");
		return "redirect:/vendor/promotions";
	}

	@PostMapping("/{id}/delete")
	public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		promotionService.deactivateCurrentShopPromotion(id);
		redirectAttributes.addFlashAttribute("successMessage", "Đã ngừng chương trình khuyến mãi");
		return "redirect:/vendor/promotions";
	}

	private String save(Long promotionId, VendorPromotionRequest request, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareForm(model, promotionId);
			return FORM_VIEW;
		}
		try {
			promotionService.saveCurrentShopPromotion(promotionId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã lưu chương trình khuyến mãi");
			return "redirect:/vendor/promotions";
		} catch (BadRequestException exception) {
			if ("Ngày kết thúc phải sau ngày bắt đầu".equals(exception.getMessage())) {
				bindingResult.rejectValue("endDate", "promotion.dateRange", exception.getMessage());
			} else {
				bindingResult.reject("promotion.invalid", exception.getMessage());
			}
			prepareForm(model, promotionId);
			return FORM_VIEW;
		}
	}

	private void prepareForm(Model model, Long promotionId) {
		model.addAttribute("promotionId", promotionId);
		model.addAttribute("promotionAction", promotionId == null
				? "/vendor/promotions/new" : "/vendor/promotions/" + promotionId + "/edit");
		model.addAttribute("productOptions", promotionService.getCurrentShopProductOptions());
		prepareLayout(model, promotionId == null ? "Tạo chương trình khuyến mãi" : "Sửa chương trình khuyến mãi",
				"/vendor/promotions");
	}

	private void prepareLayout(Model model, String title, String currentPath) {
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", currentPath);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Khuyến mãi", "url", "/vendor/promotions"),
				Map.of("label", title, "url", currentPath)));
	}
}
