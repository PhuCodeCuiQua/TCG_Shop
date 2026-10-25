package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.WishlistToggleResponse;
import com.tcgshop.service.WishlistService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class WishlistRestController {
	private final WishlistService wishlistService;

	@PostMapping("/toggle/{productId}")
	public ResponseEntity<WishlistToggleResponse> toggle(@PathVariable Long productId) {
		return ResponseEntity.ok(wishlistService.toggle(productId));
	}

	@GetMapping("/status")
	public ResponseEntity<Map<Long, Boolean>> statuses(@RequestParam List<Long> productIds) {
		return ResponseEntity.ok(wishlistService.getStatuses(productIds));
	}
}
