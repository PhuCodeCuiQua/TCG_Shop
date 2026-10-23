package com.tcgshop.controller.web;

import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.service.ShopRegistrationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class VendorDashboardController {
	private final ShopRegistrationService shopRegistrationService;

	@GetMapping("/vendor/dashboard")
	public String dashboard(Model model, RedirectAttributes redirectAttributes) {
		ShopRegistrationResponse shop = shopRegistrationService.getCurrentUserShop().orElse(null);
		if (shop == null || shop.status() != ShopStatus.APPROVED) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Kênh người bán chỉ dành cho shop đã được duyệt.");
			return "redirect:/seller-registration/status";
		}

		model.addAttribute("dashboardRole", "VENDOR");
		model.addAttribute("dashboardTitle", "Dashboard người bán");
		model.addAttribute("currentPath", "/vendor/dashboard");
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Dashboard", "url", "/vendor/dashboard"),
				Map.of("label", "VENDOR", "url", "/vendor/dashboard")));
		return "dashboard/demo-vendor";
	}
}
