package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.request.SelectAllRequest;
import com.tcgshop.dto.request.UpdateCartItemRequest;
import com.tcgshop.dto.request.UpdateSelectedRequest;
import com.tcgshop.dto.response.AddToCartResponse;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.service.CartService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints only delegate cart operations to CartService. */
@RestController
@RequestMapping("/api/cart")
public class CartRestController {

	private final CartService cartService;

	public CartRestController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public ResponseEntity<CartResponse> getCart() {
		return ResponseEntity.ok(cartService.getCart());
	}

	@GetMapping("/count")
	public ResponseEntity<Map<String, Integer>> getCartCount() {
		return ResponseEntity.ok(Map.of("cartCount", cartService.getCartCount()));
	}

	@PostMapping("/items")
	public ResponseEntity<AddToCartResponse> addItem(@Valid @RequestBody AddToCartRequest request) {
		return ResponseEntity.ok(cartService.addItem(request));
	}

	@PutMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> updateQuantity(@PathVariable Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request) {
		return ResponseEntity.ok(cartService.updateQuantity(itemId, request));
	}

	@PatchMapping("/items/{itemId}/selected")
	public ResponseEntity<CartResponse> updateSelected(@PathVariable Long itemId,
			@Valid @RequestBody UpdateSelectedRequest request) {
		return ResponseEntity.ok(cartService.updateSelected(itemId, request));
	}

	@PatchMapping("/select-all")
	public ResponseEntity<CartResponse> selectAll(@Valid @RequestBody SelectAllRequest request) {
		return ResponseEntity.ok(cartService.selectAll(request));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
		return ResponseEntity.ok(cartService.removeItem(itemId));
	}

	@DeleteMapping("/unavailable")
	public ResponseEntity<CartResponse> removeUnavailableItems() {
		return ResponseEntity.ok(cartService.removeUnavailableItems());
	}
}
