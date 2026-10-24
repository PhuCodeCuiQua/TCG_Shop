package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.VendorReturnRequest;
import com.tcgshop.dto.response.VendorReturnActionResponse;
import com.tcgshop.service.VendorReturnService;
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
@RequestMapping("/api/vendor/returns")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorReturnRestController {
	private final VendorReturnService vendorReturnService;

	@PostMapping("/{orderId}/approve")
	public ResponseEntity<VendorReturnActionResponse> approve(@PathVariable Long orderId) {
		vendorReturnService.approve(orderId);
		return ResponseEntity.ok(new VendorReturnActionResponse("Đã chấp nhận yêu cầu trả hàng"));
	}

	@PostMapping("/{orderId}/reject")
	public ResponseEntity<VendorReturnActionResponse> reject(@PathVariable Long orderId,
			@Valid @RequestBody VendorReturnRequest request) {
		vendorReturnService.reject(orderId, request.reason());
		return ResponseEntity.ok(new VendorReturnActionResponse("Đã từ chối yêu cầu trả hàng"));
	}
}
