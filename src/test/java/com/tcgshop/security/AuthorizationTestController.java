package com.tcgshop.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints used only by the MVC authorization tests. */
@RestController
class AuthorizationTestController {
	@GetMapping("/error")
	String errorAuthentication() {
		return org.springframework.security.core.context.SecurityContextHolder.getContext()
				.getAuthentication().getName();
	}
	@GetMapping({"/vendor/ping", "/shipper/ping", "/manager/ping", "/admin/ping", "/cart/ping",
			"/profile/ping", "/api/vendor/ping", "/api/admin/ping", "/api/manager/ping",
			"/api/public-test", "/api/products/ping"})
	String getPing() { return "ok"; }

	@PostMapping("/vendor/ping")
	String postVendorPing() { return "ok"; }

	@PostMapping("/payment/momo/ipn")
	String momoIpn() { return "ok"; }
}
