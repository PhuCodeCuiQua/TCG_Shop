package com.tcgshop.controller.web;

import com.tcgshop.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class WishlistController {
	private static final int PAGE_SIZE = 12;
	private final WishlistService wishlistService;

	@GetMapping("/wishlist")
	public String wishlist(@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("wishlistPage", wishlistService.getCurrentUserWishlist(
				PageRequest.of(Math.max(0, page), PAGE_SIZE)));
		return "user/wishlist";
	}
}
