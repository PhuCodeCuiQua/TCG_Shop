package com.tcgshop.controller.web;

import com.tcgshop.dto.request.CommissionRateRequest;
import com.tcgshop.dto.response.ShopCommissionResponse;
import com.tcgshop.service.CommissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/commissions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCommissionController {

	private static final int PAGE_SIZE = 10;
	private static final String BASE_URL = "/admin/commissions";
	private static final String LIST_VIEW = "admin/commissions/list";
	private static final String SHOP_FORM_VIEW = "admin/commissions/shop-form";

	private final CommissionService commissionService;

	@GetMapping
	public String list(
			@RequestParam(defaultValue = "0") int historyPage,
			@RequestParam(defaultValue = "0") int shopPage,
			Model model) {
		loadListModel(model, historyPage, shopPage);
		return LIST_VIEW;
	}

	@PostMapping("/default")
	public String updateDefaultRate(
			@Valid @ModelAttribute("defaultRateRequest") CommissionRateRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			commissionService.addDefaultRate(request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tỷ lệ hoa hồng mặc định mới");
			return "redirect:" + BASE_URL;
		}
		loadListModel(model, 0, 0);
		return LIST_VIEW;
	}

	@GetMapping("/shops/{shopId}")
	public String editShopRate(@PathVariable Long shopId, Model model) {
		var shopRate = commissionService.getShopCommissionRate(shopId);
		CommissionRateRequest request = new CommissionRateRequest();
		request.setRate(shopRate.overrideRate());
		prepareShopForm(model, shopId, request, shopRate);
		return SHOP_FORM_VIEW;
	}

	@PostMapping("/shops/{shopId}")
	public String updateShopRate(
			@PathVariable Long shopId,
			@Valid @ModelAttribute("shopRateRequest") CommissionRateRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			commissionService.setShopRate(shopId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tỷ lệ hoa hồng riêng cho cửa hàng");
			return "redirect:" + BASE_URL;
		}
		prepareShopForm(model, shopId, request, commissionService.getShopCommissionRate(shopId));
		return SHOP_FORM_VIEW;
	}

	@PostMapping("/shops/{shopId}/reset")
	public String resetShopRate(@PathVariable Long shopId, RedirectAttributes redirectAttributes) {
		commissionService.resetShopRate(shopId);
		redirectAttributes.addFlashAttribute("successMessage",
				"Chi nhánh đã sử dụng lại tỷ lệ hoa hồng mặc định của hệ thống");
		return "redirect:" + BASE_URL;
	}

	private void loadListModel(Model model, int historyPage, int shopPage) {
		model.addAttribute("defaultRate", commissionService.getDefaultRate());
		model.addAttribute("settingsHistory", commissionService.getSettingsHistory(
				PageRequest.of(Math.max(historyPage, 0), PAGE_SIZE, Sort.by("createdAt").descending())));
		model.addAttribute("shopRates", commissionService.getShopCommissionRates(
				PageRequest.of(Math.max(shopPage, 0), PAGE_SIZE)));
		if (!model.containsAttribute("defaultRateRequest")) {
			model.addAttribute("defaultRateRequest", new CommissionRateRequest());
		}
		addLayoutModel(model, "Quản lý hoa hồng");
	}

	private void prepareShopForm(
			Model model, Long shopId, CommissionRateRequest request, ShopCommissionResponse shopRate) {
		model.addAttribute("shopRate", shopRate);
		model.addAttribute("shopRateRequest", request);
		model.addAttribute("defaultRate", commissionService.getDefaultRate());
		model.addAttribute("formAction", BASE_URL + "/shops/" + shopId);
		addLayoutModel(model, "Điều chỉnh hoa hồng cửa hàng");
	}

	private void addLayoutModel(Model model, String title) {
		model.addAttribute("dashboardRole", "ADMIN");
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản trị", "url", "/admin/dashboard"),
				Map.of("label", "Hoa hồng", "url", BASE_URL)));
	}
}
