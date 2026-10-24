package com.tcgshop.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.ShopRegistrationRequest;
import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.service.ShopRegistrationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class SellerRegistrationControllerTest {
	@Mock
	private ShopRegistrationService shopRegistrationService;

	@InjectMocks
	private SellerRegistrationController controller;

	@Test
	void registrationPageRendersOnlyWhenUserHasNoShop() {
		ExtendedModelMap model = new ExtendedModelMap();
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());

		assertEquals("seller-registration/register", controller.showRegistrationForm(model));
		assertInstanceOf(ShopRegistrationRequest.class, model.getAttribute("shopRegistrationRequest"));
	}

	@Test
	void registrationPageRedirectsEveryExistingShopToStatus() {
		for (ShopStatus status : ShopStatus.values()) {
			when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop(status)));

			assertEquals("redirect:/seller-registration/status",
					controller.showRegistrationForm(new ExtendedModelMap()), status.name());
		}
	}

	@Test
	void resubmitPagePrefillsOnlyRejectedShop() {
		ExtendedModelMap model = new ExtendedModelMap();
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop(ShopStatus.REJECTED)));

		assertEquals("seller-registration/register", controller.showResubmissionForm(model));
		ShopRegistrationRequest request = (ShopRegistrationRequest) model.getAttribute("shopRegistrationRequest");
		assertEquals("Moonlight Cards", request.getName());
		assertEquals("Collector shop", request.getDescription());
		assertEquals("0912345678", request.getPhoneNumber());
		assertEquals("Hà Nội", request.getPickupAddress());
		assertEquals("/logos/moonlight.png", model.getAttribute("existingLogoUrl"));
	}

	@Test
	void resubmitPageRejectsMissingOrNonRejectedShop() {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());
		assertEquals("redirect:/seller-registration/status",
				controller.showResubmissionForm(new ExtendedModelMap()));

		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop(ShopStatus.PENDING)));
		assertEquals("redirect:/seller-registration/status",
				controller.showResubmissionForm(new ExtendedModelMap()));
	}

	@Test
	void statusPageSuppliesCurrentShopForTheStatusCard() {
		ExtendedModelMap model = new ExtendedModelMap();
		ShopRegistrationResponse shop = shop(ShopStatus.APPROVED);
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop));

		assertEquals("seller-registration/status", controller.showStatus(model));
		assertEquals(shop, model.getAttribute("shop"));
		verify(shopRegistrationService).getCurrentUserShop();
	}

	@Test
	void statusPageSupportsUserWithoutShop() {
		ExtendedModelMap model = new ExtendedModelMap();
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());

		assertEquals("seller-registration/status", controller.showStatus(model));
		assertTrue(model.containsAttribute("shop"));
	}

	private ShopRegistrationResponse shop(ShopStatus status) {
		return new ShopRegistrationResponse(
				11L, "Moonlight Cards", "Collector shop", "/logos/moonlight.png",
				"0912345678", "Hà Nội", status, "Vui lòng cập nhật địa chỉ.");
	}
}
