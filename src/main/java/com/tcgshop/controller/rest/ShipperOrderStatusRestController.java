package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.ShipperOrderStatusChangeRequest;
import com.tcgshop.dto.response.ShipperOrderStatusChangeResponse;
import com.tcgshop.service.ShipperOrderStatusService;
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
@RequestMapping("/shipper/orders/{orderId}/status")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperOrderStatusRestController {
	private final ShipperOrderStatusService shipperOrderStatusService;

	@PostMapping
	public ResponseEntity<ShipperOrderStatusChangeResponse> changeStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody ShipperOrderStatusChangeRequest request) {
		return ResponseEntity.ok(shipperOrderStatusService.changeStatus(orderId, request));
	}
}
