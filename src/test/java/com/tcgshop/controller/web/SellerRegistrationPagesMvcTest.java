package com.tcgshop.controller.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.service.ShopRegistrationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SellerRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SellerRegistrationPagesMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ShopRegistrationService shopRegistrationService;

	@Test
	void registrationPageUsesSharedLayoutAndRendersSellerBenefitsAndForm() throws Exception {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());

		mockMvc.perform(get("/seller-registration").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(view().name("seller-registration/register"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Mở shop TCG của bạn")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Gửi đăng ký shop")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("DÀNH CHO NHÀ SƯU TẦM")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("navbar-brand fw-bold")));
	}

	@Test
	void rejectedStatusPageShowsReasonAndResubmitRoute() throws Exception {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(rejectedShop()));

		mockMvc.perform(get("/seller-registration/status").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(view().name("seller-registration/status"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ bị từ chối")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng cập nhật địa chỉ lấy hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/seller-registration/resubmit\"")));
	}

	@Test
	void statusCardRendersPendingApprovedAndSuspendedStates() throws Exception {
		assertStatusState(ShopStatus.PENDING, "Đang chờ duyệt", "M24 13v11l7 4");
		assertStatusState(ShopStatus.APPROVED, "Đã duyệt", "href=\"/vendor/dashboard\"");
		assertStatusState(ShopStatus.SUSPENDED, "Shop đã bị khóa", "rect x=\"13\" y=\"21\"");
	}

	@Test
	void rejectedResubmissionPageRendersThePrepopulatedForm() throws Exception {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(rejectedShop()));

		mockMvc.perform(get("/seller-registration/resubmit").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(view().name("seller-registration/register"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Cập nhật hồ sơ shop")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Moonlight Cards")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"name\"")));
	}

	private void assertStatusState(ShopStatus status, String expectedCopy, String expectedMarkup) throws Exception {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(shop(status)));

		mockMvc.perform(get("/seller-registration/status").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(expectedCopy)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(expectedMarkup)));
	}

	private DefaultCsrfToken csrfToken() {
		return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
	}

	private ShopRegistrationResponse rejectedShop() {
		return new ShopRegistrationResponse(
				11L, "Moonlight Cards", "Collector shop", "/logos/moonlight.png",
				"0912345678", "Hà Nội", ShopStatus.REJECTED, "Vui lòng cập nhật địa chỉ lấy hàng.");
	}

	private ShopRegistrationResponse shop(ShopStatus status) {
		return new ShopRegistrationResponse(
				11L, "Moonlight Cards", "Collector shop", "/logos/moonlight.png",
				"0912345678", "Hà Nội", status, "Vui lòng cập nhật địa chỉ lấy hàng.");
	}
}
