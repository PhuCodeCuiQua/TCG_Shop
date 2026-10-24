package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.PlaceOrderResponse;
import com.tcgshop.service.CheckoutService;
import com.tcgshop.service.OrderPlacementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
	private final OrderPlacementService orderPlacementService;

	public CheckoutRestController(CheckoutService checkoutService, OrderPlacementService orderPlacementService) {
		this.checkoutService = checkoutService;
		this.orderPlacementService = orderPlacementService;
	}

	@PostMapping("/preview")
	public ResponseEntity<CheckoutPreviewResponse> preview(
			@Valid @RequestBody CheckoutPreviewRequest request) {
		return ResponseEntity.ok(checkoutService.preview(request));
	}

	@PostMapping("/place")
	public ResponseEntity<PlaceOrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
		PlaceOrderResponse response = orderPlacementService.placeOrder(request);
		if (response.alreadyPlaced()) return ResponseEntity.ok(response);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.LOCATION, response.redirectUrl())
				.body(response);
	}
}
