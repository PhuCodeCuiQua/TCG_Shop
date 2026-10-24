package com.tcgshop.controller.web;

import com.tcgshop.dto.request.ShopRegistrationRequest;
import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.ShopRegistrationService;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller-registration")
@RequiredArgsConstructor
public class SellerRegistrationController {
	private static final String REGISTER_VIEW = "seller-registration/register";
	private static final String STATUS_VIEW = "seller-registration/status";

	private final ShopRegistrationService shopRegistrationService;

	@GetMapping
	public String showRegistrationForm(Model model) {
		Optional<ShopRegistrationResponse> existingShop = shopRegistrationService.getCurrentUserShop();
		if (existingShop.isPresent()) {
			return "redirect:/seller-registration/status";
		}
		if (!model.containsAttribute("shopRegistrationRequest")) {
			model.addAttribute("shopRegistrationRequest", new ShopRegistrationRequest());
		}
		return REGISTER_VIEW;
	}

	@GetMapping("/resubmit")
	public String showResubmissionForm(Model model) {
		Optional<ShopRegistrationResponse> existingShop = shopRegistrationService.getCurrentUserShop();
		if (existingShop.isEmpty() || existingShop.get().status() != ShopStatus.REJECTED) {
			return "redirect:/seller-registration/status";
		}
		populateRejectedForm(existingShop.get(), model);
		return REGISTER_VIEW;
	}

	@PostMapping
	public String register(
			@Valid @ModelAttribute("shopRegistrationRequest") ShopRegistrationRequest request,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasErrors()) {
			try {
				shopRegistrationService.registerOrResubmit(request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đăng ký shop. Hồ sơ đang chờ duyệt.");
				return "redirect:/seller-registration/status";
			} catch (ConflictException exception) {
				bindingResult.reject("shop.registration.conflict", exception.getMessage());
			}
		}
		model.addAttribute("shopRegistrationRequest", request);
		shopRegistrationService.getCurrentUserShop()
				.filter(shop -> shop.status() == ShopStatus.REJECTED)
				.map(ShopRegistrationResponse::logoUrl)
				.ifPresent(logoUrl -> model.addAttribute("existingLogoUrl", logoUrl));
		return REGISTER_VIEW;
	}

	@GetMapping("/status")
	public String showStatus(Model model) {
		model.addAttribute("shop", shopRegistrationService.getCurrentUserShop().orElse(null));
		return STATUS_VIEW;
	}

	private void populateRejectedForm(ShopRegistrationResponse shop, Model model) {
		if (model.containsAttribute("shopRegistrationRequest")) {
			return;
		}
		ShopRegistrationRequest request = new ShopRegistrationRequest();
		request.setName(shop.name());
		request.setDescription(shop.description());
		request.setPhoneNumber(shop.phoneNumber());
		request.setPickupAddress(shop.pickupAddress());
		model.addAttribute("shopRegistrationRequest", request);
		model.addAttribute("existingLogoUrl", shop.logoUrl());
	}
}
