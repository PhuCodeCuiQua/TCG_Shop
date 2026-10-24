package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.ReconciliationReportResponse;
import com.tcgshop.service.OrderReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReconciliationRestController {
	private final OrderReconciliationService reconciliationService;

	public AdminReconciliationRestController(OrderReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@GetMapping
	public ResponseEntity<ReconciliationReportResponse> reconcile() {
		return ResponseEntity.ok(reconciliationService.reconcile());
	}
}
