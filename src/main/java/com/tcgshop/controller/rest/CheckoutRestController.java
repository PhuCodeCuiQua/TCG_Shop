package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.MockMomoPaymentResponse;
import com.tcgshop.dto.response.PlaceOrderResponse;
import com.tcgshop.service.CheckoutService;
import com.tcgshop.service.MockMomoPaymentService;
import com.tcgshop.service.OrderPlacementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final MockMomoPaymentService mockMomoPaymentService;

	public CheckoutRestController(CheckoutService checkoutService, OrderPlacementService orderPlacementService,
			MockMomoPaymentService mockMomoPaymentService) {
		this.checkoutService = checkoutService;
		this.orderPlacementService = orderPlacementService;
		this.mockMomoPaymentService = mockMomoPaymentService;
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

	@PostMapping("/{groupCode}/mock-momo/confirm")
	public ResponseEntity<MockMomoPaymentResponse> confirmMockMomo(@PathVariable String groupCode) {
		return ResponseEntity.ok(mockMomoPaymentService.confirm(groupCode));
	}

	@PostMapping("/{groupCode}/mock-momo/cancel")
	public ResponseEntity<MockMomoPaymentResponse> cancelMockMomo(@PathVariable String groupCode) {
		return ResponseEntity.ok(mockMomoPaymentService.cancel(groupCode));
	}
}
