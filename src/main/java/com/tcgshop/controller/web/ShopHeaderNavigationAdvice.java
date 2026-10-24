package com.tcgshop.controller.web;

import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.service.ShopRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.tcgshop.controller.web")
@RequiredArgsConstructor
public class ShopHeaderNavigationAdvice {
	private final ObjectProvider<ShopRegistrationService> shopRegistrationServiceProvider;

	@ModelAttribute("currentUserShop")
	public ShopRegistrationResponse addCurrentUserShop(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken
				|| !hasShopAccountRole(authentication)) {
			return null;
		}
		ShopRegistrationService shopRegistrationService = shopRegistrationServiceProvider.getIfAvailable();
		if (shopRegistrationService == null) {
			return null;
		}
		return shopRegistrationService.getCurrentUserShop().orElse(null);
	}

	private boolean hasShopAccountRole(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(role -> "ROLE_USER".equals(role) || "ROLE_VENDOR".equals(role));
	}
}
