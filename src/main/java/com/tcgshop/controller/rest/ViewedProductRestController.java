package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.ViewedProductRecordResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ViewedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewed-products")
@RequiredArgsConstructor
public class ViewedProductRestController {
	private final ViewedProductService viewedProductService;
	private final CurrentUser currentUser;

	@PostMapping("/record/{productId}")
	public ResponseEntity<ViewedProductRecordResponse> record(@PathVariable Long productId) {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER)) {
			throw new ForbiddenException("Chức năng này chỉ dành cho tài khoản người mua");
		}
		viewedProductService.record(currentUser.getId(), productId);
		return ResponseEntity.ok(new ViewedProductRecordResponse(true));
	}
}
