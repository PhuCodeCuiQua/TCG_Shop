package com.tcgshop.controller.web;

import com.tcgshop.dto.request.VendorShopManageRequest;
import com.tcgshop.dto.response.VendorShopManageResponse;
import com.tcgshop.service.VendorShopManageService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendor/shop/manage")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorShopManageController {
	private static final String VIEW = "vendor/shop-manage";
	private static final String SHOP_MANAGE_URL = "/vendor/shop/manage";

	private final VendorShopManageService vendorShopManageService;

	@GetMapping
	public String showForm(Model model, RedirectAttributes redirectAttributes) {
		VendorShopManageResponse shop = vendorShopManageService.getCurrentVendorShop().orElse(null);
		if (shop == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa có hồ sơ chi nhánh.");
			return "redirect:/seller-registration";
		}
		model.addAttribute("shop", shop);
		if (!model.containsAttribute("vendorShopManageRequest")) {
			model.addAttribute("vendorShopManageRequest", toRequest(shop));
		}
		addLayoutModel(model);
		return VIEW;
	}

	@PostMapping
	public String update(
			@Valid @ModelAttribute("vendorShopManageRequest") VendorShopManageRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		VendorShopManageResponse currentShop = vendorShopManageService.getCurrentVendorShop().orElse(null);
		if (currentShop == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa có hồ sơ chi nhánh.");
			return "redirect:/seller-registration";
		}

		if (!bindingResult.hasErrors()) {
			vendorShopManageService.updateCurrentVendorShop(request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin chi nhánh.");
			return "redirect:" + SHOP_MANAGE_URL;
		}

		model.addAttribute("shop", currentShop);
		addLayoutModel(model);
		return VIEW;
	}

	private VendorShopManageRequest toRequest(VendorShopManageResponse shop) {
		VendorShopManageRequest request = new VendorShopManageRequest();
		request.setDescription(shop.description());
		request.setPhoneNumber(shop.phoneNumber());
		request.setPickupAddress(shop.pickupAddress());
		return request;
	}

	private void addLayoutModel(Model model) {
		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Thông tin Chi nhánh");
		model.addAttribute("currentPath", SHOP_MANAGE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý Chi nhánh", "url", "/vendor/dashboard"),
				Map.of("label", "Thông tin Chi nhánh", "url", SHOP_MANAGE_URL)));
	}
}
