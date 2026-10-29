package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.VendorOrderStatusChangeRequest;
import com.tcgshop.dto.response.VendorOrderStatusResponse;
import com.tcgshop.service.VendorOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/orders")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorOrderRestController {
	private final VendorOrderService vendorOrderService;

	@PostMapping("/{orderId}/status")
	public ResponseEntity<VendorOrderStatusResponse> changeStatus(@PathVariable Long orderId,
			@Valid @RequestBody VendorOrderStatusChangeRequest request) {
		return ResponseEntity.ok(vendorOrderService.changeOrderStatus(orderId, request));
	}
}
