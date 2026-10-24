package com.tcgshop.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.service.ShopRegistrationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ShopHeaderNavigationAdviceTest {
	@Mock
	private ShopRegistrationService shopRegistrationService;

	@Mock
	private ObjectProvider<ShopRegistrationService> shopRegistrationServiceProvider;

	@Test
	void suppliesShopStatusForAuthenticatedUserNavigation() {
		ShopRegistrationResponse shop = new ShopRegistrationResponse(
				1L, "Moonlight Cards", null, null, "0912345678", "Hà Nội", ShopStatus.PENDING, null);
		when(shopRegistrationServiceProvider.getIfAvailable()).thenReturn(shopRegistrationService);
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop));

		assertEquals(shop, advice().addCurrentUserShop(authentication("ROLE_USER")));
		verify(shopRegistrationService).getCurrentUserShop();
	}

	@Test
	void suppliesNullWhenUserHasNotRegisteredAShop() {
		when(shopRegistrationServiceProvider.getIfAvailable()).thenReturn(shopRegistrationService);
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());

		assertNull(advice().addCurrentUserShop(authentication("ROLE_USER")));
	}

	@Test
	void doesNotQueryShopForAnonymousOrUnrelatedRoles() {
		AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
				"test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

		assertNull(advice().addCurrentUserShop(anonymous));
		assertNull(advice().addCurrentUserShop(authentication("ROLE_SHIPPER")));
		verifyNoInteractions(shopRegistrationService);
		verifyNoInteractions(shopRegistrationServiceProvider);
	}

	private ShopHeaderNavigationAdvice advice() {
		return new ShopHeaderNavigationAdvice(shopRegistrationServiceProvider);
	}

	private UsernamePasswordAuthenticationToken authentication(String role) {
		return new UsernamePasswordAuthenticationToken(
				"test-user", "n/a", List.of(new SimpleGrantedAuthority(role)));
	}
}
