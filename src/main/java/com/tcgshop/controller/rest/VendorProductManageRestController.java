package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.VendorProductActionResponse;
import com.tcgshop.service.VendorProductManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendor/products")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorProductManageRestController {
	private final VendorProductManageService productManageService;

	@PostMapping("/{id}/toggle-status")
	public ResponseEntity<VendorProductActionResponse> toggleStatus(@PathVariable Long id) {
		return ResponseEntity.ok(productManageService.toggleOwnedProduct(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<VendorProductActionResponse> softDelete(@PathVariable Long id) {
		return ResponseEntity.ok(productManageService.softDeleteOwnedProduct(id));
	}
}
