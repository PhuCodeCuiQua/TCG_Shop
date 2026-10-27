package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.dto.response.PublicShopResponse;
import com.tcgshop.dto.response.ReviewProductResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PublicShopService;
import com.tcgshop.service.ReviewEligibilityService;
import com.tcgshop.service.ReviewService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ReviewPageController.class, PublicShopController.class})
@AutoConfigureMockMvc(addFilters = false)
class PublicRatingControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@MockBean private ReviewService reviewService;
	@MockBean private ReviewEligibilityService eligibilityService;
	@MockBean private PublicShopService publicShopService;
	@MockBean private CurrentUser currentUser;

	@BeforeEach
	void setAnonymousContext() {
		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.AnonymousAuthenticationToken("test", "anonymousUser",
						org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
	}

	@AfterEach
	void clearContext() {
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

	@Test
	void emptyReviewsRenderPublicFragmentInsteadOfError() throws Exception {
		stubEmptyReviews();
		mockMvc.perform(get("/products/7/reviews").header("X-Requested-With", "XMLHttpRequest"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-review-section")))
				.andExpect(content().string(containsString("Chưa có đánh giá nào")))
				.andExpect(content().string(containsString("Đăng nhập")));
	}

	@Test
	void deliveredBuyerSeesFormAndIneligibleBuyerDoesNot() throws Exception {
		stubEmptyReviews();
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(12L);
		when(eligibilityService.canReview(12L, 7L)).thenReturn(true);
		mockMvc.perform(get("/products/7/reviews"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("class=\"review-form\"")));
		when(eligibilityService.canReview(12L, 7L)).thenReturn(false);
		when(eligibilityService.getIneligibilityMessage(12L, 7L)).thenReturn("Đơn hàng chưa được giao.");
		mockMvc.perform(get("/products/7/reviews"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*class=\"review-form\\s+d-none\".*")))
				.andExpect(content().string(containsString("Đơn hàng chưa được giao.")));
	}

	@Test
	void unratedShopDoesNotClaimFiveStars() throws Exception {
		stubShop(BigDecimal.ZERO, 0);
		mockMvc.perform(get("/shops/3").requestAttr("_csrf", csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Chưa có đánh giá")))
				.andExpect(content().string(not(containsString("Shop mới"))))
				.andExpect(content().string(not(containsString(">★</span>"))));
	}

	@Test
	void shopWithMissingAverageDoesNotShowStars() throws Exception {
		stubShop(null, 8);
		mockMvc.perform(get("/shops/3").requestAttr("_csrf", csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Chưa có đánh giá")))
				.andExpect(content().string(not(containsString(">★</span>"))));
	}

	@Test
	void existingReviewsRenderWithPagination() throws Exception {
		stubEmptyReviews();
		var review = new com.tcgshop.dto.response.ReviewResponse(20L, 12L, "Người mua", null,
				4, null, java.time.LocalDateTime.of(2026, 1, 1, 12, 0), List.of(), List.of());
		when(reviewService.getReviewsByProduct(eq(7L), any()))
				.thenReturn(new PageResponse<>(List.of(review), 0, 5, 6, 2, true, false));
		mockMvc.perform(get("/products/7/reviews"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Người mua")))
				.andExpect(content().string(containsString("data-review-id=\"20\"")))
				.andExpect(content().string(containsString("data-pagination-page=\"1\"")));
	}

	@Test
	void ratedShopShowsItsActualScoreAndCount() throws Exception {
		stubShop(new BigDecimal("4.3"), 8);
		mockMvc.perform(get("/shops/3").requestAttr("_csrf", csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*4[.,]3.*")))
				.andExpect(content().string(containsString("(8 đánh giá)")));
	}

	private void stubEmptyReviews() {
		when(reviewService.getProductSummary(7L))
				.thenReturn(new ReviewProductResponse(7L, "Test card", BigDecimal.ZERO, 0));
		when(reviewService.getReviewsByProduct(eq(7L), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 5, 0, 0, true, true));
	}

	private void stubShop(BigDecimal average, Integer count) {
		PublicShopResponse shop = new PublicShopResponse(3L, "Test shop", "test-shop", null,
				null, null, null, null, average, count);
		when(publicShopService.getApprovedShopPage(eq(3L), any(), any()))
				.thenReturn(new PublicShopPageResponse(shop, Page.empty()));
	}

	private DefaultCsrfToken csrf() {
		return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
	}
}
