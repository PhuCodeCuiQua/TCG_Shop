package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint chỉ chuyển yêu cầu xem trước sang CheckoutService. */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutRestController {
	private final CheckoutService checkoutService;

	public CheckoutRestController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/preview")
	public ResponseEntity<CheckoutPreviewResponse> preview(
			@Valid @RequestBody CheckoutPreviewRequest request) {
		return ResponseEntity.ok(checkoutService.preview(request));
	}
}
