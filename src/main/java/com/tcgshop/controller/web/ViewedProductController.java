package com.tcgshop.controller.web;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ViewedProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ViewedProductController {
	private static final int PAGE_SIZE = 8;

	private final ViewedProductQueryService viewedProductQueryService;
	private final CurrentUser currentUser;

	@GetMapping("/viewed-products")
	public String viewedProducts(@RequestParam(defaultValue = "0") int page, Model model) {
		if (!currentUser.isAuthenticated()) {
			return "redirect:/auth/login?returnUrl=%2Fviewed-products";
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER)) {
			throw new ForbiddenException("Chức năng này chỉ dành cho tài khoản người mua");
		}
		model.addAttribute("productsPage", viewedProductQueryService.getRecentProducts(
				currentUser.getId(), PageRequest.of(Math.max(0, page), PAGE_SIZE)));
		return "user/viewed-products";
	}
}
