package com.tcgshop.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.controller.rest.CheckoutRestController;
import com.tcgshop.controller.web.CheckoutController;
import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.PlaceOrderResponse;
import com.tcgshop.dto.response.PlacedOrderSummaryResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CheckoutService;
import com.tcgshop.service.OrderPlacementService;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {CheckoutController.class, CheckoutRestController.class,
        com.tcgshop.controller.rest.CheckoutCouponRestController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class CheckoutControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private CheckoutService checkoutService;
	@MockBean private CartService cartService;
	@MockBean private OrderPlacementService orderPlacementService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private com.tcgshop.service.CouponSuggestionService couponSuggestionService;

	@Test
	void suggestionsRequireBuyerAndReturnAnEmptyListWhenNoneApply() throws Exception {
		when(couponSuggestionService.getSuggestions()).thenReturn(List.of());
		mockMvc.perform(get("/api/checkout/coupons"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/checkout/coupons").cookie(accessCookie("manager@example.com")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/checkout/coupons").cookie(accessCookie("user@example.com")))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void suggestionsExposeCouponDetailsWithoutEntities() throws Exception {
		when(couponSuggestionService.getSuggestions()).thenReturn(List.of(
				new com.tcgshop.dto.response.CouponSuggestionResponse(1L, 7L, "SAVE",
						com.tcgshop.entity.enums.DiscountType.PERCENTAGE, BigDecimal.TEN,
						new BigDecimal("100000"), new BigDecimal("20000"),
						java.time.LocalDateTime.of(2026, 12, 31, 23, 59), new BigDecimal("10000"))));
		mockMvc.perform(get("/api/checkout/coupons").cookie(accessCookie("user@example.com")))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("SAVE"))
				.andExpect(jsonPath("$[0].shopId").value(7))
				.andExpect(jsonPath("$[0].discountAmount").value(10000))
				.andExpect(jsonPath("$[0].shop").doesNotExist());
	}

	@BeforeEach
	void configureUsers() {
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation ->
				details(invocation.getArgument(0)));
		when(cartService.getCart()).thenReturn(cart(1));
		when(checkoutService.preview(any(CheckoutPreviewRequest.class))).thenReturn(emptyPreview());
		when(orderPlacementService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(placed(false));
	}

	@Test
	void placeRequiresUserRoleAndCsrfAndReturnsCreatedOrOkForIdempotentReplay() throws Exception {
		mockMvc.perform(post("/api/checkout/place").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
		for (String email : List.of("manager@example.com", "shipper@example.com")) {
			mockMvc.perform(post("/api/checkout/place").cookie(accessCookie(email)).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
					.andExpect(status().isForbidden());
		}
		Cookie user = accessCookie("user@example.com");
		mockMvc.perform(post("/api/checkout/place").cookie(user)
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/checkout/place").cookie(user).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isCreated()).andExpect(header().string("Location", "/checkout/success/GRP-1"))
				.andExpect(jsonPath("$.groupCode").value("GRP-1"))
				.andExpect(jsonPath("$.alreadyPlaced").value(false));
		org.mockito.ArgumentCaptor<PlaceOrderRequest> requestCaptor =
				org.mockito.ArgumentCaptor.forClass(PlaceOrderRequest.class);
		verify(orderPlacementService).placeOrder(requestCaptor.capture());
		assertTrue(requestCaptor.getValue().note().equals("Giao buổi chiều"));
		when(orderPlacementService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(placed(true));
		mockMvc.perform(post("/api/checkout/place").cookie(user).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.alreadyPlaced").value(true));
	}

	@Test
	void placeValidationAndBusinessFailuresReturnJsonContracts() throws Exception {
		Cookie user = accessCookie("user@example.com");
		for (String body : List.of(
				"{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\",\"idempotencyToken\":\"bad\",\"expectedGrandTotal\":1}",
				"{\"shops\":[{\"shopId\":5}],\"idempotencyToken\":\"" + UUID.randomUUID() + "\",\"expectedGrandTotal\":1}",
				"{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\",\"idempotencyToken\":\"" + UUID.randomUUID() + "\",\"expectedGrandTotal\":-1}",
				"{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\",\"idempotencyToken\":\"" + UUID.randomUUID() + "\",\"expectedGrandTotal\":1,\"note\":\"" + "n".repeat(501) + "\"}")) {
			mockMvc.perform(post("/api/checkout/place").cookie(user).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
		}
		when(orderPlacementService.placeOrder(any(PlaceOrderRequest.class))).thenThrow(new ConflictException("Tổng tiền đã đổi"));
		mockMvc.perform(post("/api/checkout/place").cookie(user).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
		when(orderPlacementService.placeOrder(any(PlaceOrderRequest.class))).thenThrow(new BusinessException("Không thể đặt hàng"));
		mockMvc.perform(post("/api/checkout/place").cookie(user).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.status").value(422));
	}

	@Test
	void successSummaryIsPrivateNoStoreAndCheckoutIsNoStore() throws Exception {
		Cookie user = accessCookie("user@example.com");
		when(orderPlacementService.getPlacedSummary("GRP-1")).thenReturn(placed(false));
		mockMvc.perform(get("/checkout/success/GRP-1").cookie(user))
				.andExpect(status().isOk()).andExpect(view().name("user/checkout-success"))
				.andExpect(model().attributeExists("summary"))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
		mockMvc.perform(get("/checkout").cookie(user))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
		when(orderPlacementService.getPlacedSummary("MISSING")).thenThrow(new ResourceNotFoundException("Đơn hàng", "MISSING"));
		mockMvc.perform(get("/checkout/success/MISSING").cookie(user))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/checkout/success/GRP-1"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/login?returnUrl=%2Fcheckout%2Fsuccess%2FGRP-1"));
	}

	@Test
	void anonymousWebRedirectsAndApiReturnsJson401() throws Exception {
		mockMvc.perform(get("/checkout"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login?returnUrl=%2Fcheckout"));
		mockMvc.perform(post("/api/checkout/preview").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void shipperAndManagerAreDeniedForWebAndApi() throws Exception {
		for (String email : List.of("shipper@example.com", "manager@example.com")) {
			Cookie token = accessCookie(email);
			mockMvc.perform(get("/checkout").cookie(token)).andExpect(status().isForbidden());
			mockMvc.perform(post("/api/checkout/preview").cookie(token).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(validBody()))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
		}
	}

	@Test
	void userAndVendorCanOpenPageAndPreview() throws Exception {
		for (String email : List.of("user@example.com", "vendor@example.com")) {
			Cookie token = accessCookie(email);
			mockMvc.perform(get("/checkout").cookie(token))
					.andExpect(status().isOk()).andExpect(view().name("user/checkout"));
			mockMvc.perform(post("/api/checkout/preview").cookie(token).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(validBody()))
					.andExpect(status().isOk()).andExpect(jsonPath("$.canPlaceOrder").value(false));
		}
	}

	@Test
	void previewRequiresCsrfAndValidatesDuplicateShopsAndCouponLength() throws Exception {
		Cookie token = accessCookie("user@example.com");
		mockMvc.perform(post("/api/checkout/preview").cookie(token)
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());

		for (String body : List.of(
				"{\"shops\":[{\"shopId\":5},{\"shopId\":5}],\"paymentMethod\":\"COD\"}",
				"{\"shops\":[{\"shopId\":5,\"couponCode\":\"" + "A".repeat(51)
						+ "\"}],\"paymentMethod\":\"COD\"}")) {
			mockMvc.perform(post("/api/checkout/preview").cookie(token).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
		}
		verify(checkoutService, never()).preview(any(CheckoutPreviewRequest.class));
	}

	@Test
	void unknownClientFieldsAreIgnoredAndOnlyRequestDtoIsPassedToService() throws Exception {
		String body = "{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\","
				+ "\"userId\":999,\"price\":1,\"grandTotal\":1}";
		mockMvc.perform(post("/api/checkout/preview").cookie(accessCookie("user@example.com")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		org.mockito.ArgumentCaptor<CheckoutPreviewRequest> captor =
				org.mockito.ArgumentCaptor.forClass(CheckoutPreviewRequest.class);
		verify(checkoutService).preview(captor.capture());
		assertTrue(captor.getValue().shops().get(0).shopId() == 5L);
		assertTrue(captor.getValue().platformCouponCode() == null);
	}

	@Test
	void emptyCartRedirectsAndNonEmptyCartCreatesFreshServerToken() throws Exception {
		when(cartService.getCart()).thenReturn(cart(0));
		mockMvc.perform(get("/checkout").cookie(accessCookie("user@example.com")))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"))
				.andExpect(flash().attribute("errorMessage", "Vui lòng chọn sản phẩm để thanh toán"));

		when(cartService.getCart()).thenReturn(cart(1));
		MvcResult first = mockMvc.perform(get("/checkout").cookie(accessCookie("user@example.com")))
				.andExpect(status().isOk()).andExpect(view().name("user/checkout"))
				.andExpect(model().attributeExists("idempotencyToken"))
				.andReturn();
		MvcResult second = mockMvc.perform(get("/checkout").cookie(accessCookie("user@example.com")))
				.andExpect(status().isOk()).andReturn();
		String firstToken = (String) first.getModelAndView().getModel().get("idempotencyToken");
		String secondToken = (String) second.getModelAndView().getModel().get("idempotencyToken");
		assertNotEquals(firstToken, secondToken);
		assertTrue(UUID.fromString(firstToken).toString().equals(firstToken));
		assertTrue(UUID.fromString(secondToken).toString().equals(secondToken));
		verify(cartService, org.mockito.Mockito.times(3)).getCart();
	}

	@Test
	void missingAddressFromPreviewReturns404JsonAndMustChangePasswordIsBlocked() throws Exception {
		when(checkoutService.preview(any(CheckoutPreviewRequest.class)))
				.thenThrow(new ResourceNotFoundException("Địa chỉ", 99L));
		mockMvc.perform(post("/api/checkout/preview").cookie(accessCookie("user@example.com")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));

		mockMvc.perform(get("/checkout").cookie(accessCookie("must-change@example.com")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
		mockMvc.perform(post("/api/checkout/preview").servletPath("/api/checkout/preview").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(details("must-change@example.com"))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	private String validBody() {
		return "{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\"}";
	}

	private String placeBody() {
		return "{\"shops\":[{\"shopId\":5}],\"paymentMethod\":\"COD\",\"idempotencyToken\":\""
				+ UUID.fromString("123e4567-e89b-42d3-a456-426614174000")
				+ "\",\"expectedGrandTotal\":100,\"note\":\"Giao buổi chiều\",\"userId\":999,\"price\":1,\"total\":1}";
	}

	private PlaceOrderResponse placed(boolean alreadyPlaced) {
		return new PlaceOrderResponse(1L, "GRP-1", PaymentMethod.COD, new BigDecimal("100"),
				List.of(new PlacedOrderSummaryResponse(2L, "ORD-1", 5L, "Shop A", new BigDecimal("100"))),
				"/checkout/success/GRP-1", alreadyPlaced);
	}

	private Cookie accessCookie(String email) {
		return new Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(details(email)));
	}

	private CustomUserDetails details(String email) {
		RoleName[] roles = switch (email) {
			case "shipper@example.com" -> new RoleName[] {RoleName.ROLE_SHIPPER};
			case "manager@example.com" -> new RoleName[] {RoleName.ROLE_MANAGER};
			case "vendor@example.com" -> new RoleName[] {RoleName.ROLE_USER, RoleName.ROLE_VENDOR};
			default -> new RoleName[] {RoleName.ROLE_USER};
		};
		User user = new User();
		user.setId(42L);
		user.setEmail(email);
		user.setFullName("Nguyen Van An");
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(email.startsWith("must-change"));
		Set<Role> userRoles = EnumSet.copyOf(List.of(roles)).stream().map(roleName -> {
			Role role = new Role();
			role.setName(roleName);
			return role;
		}).collect(java.util.stream.Collectors.toSet());
		user.setRoles(userRoles);
		return new CustomUserDetails(user);
	}

	private CartResponse cart(long selectedItemCount) {
		return new CartResponse(List.of(), 0, selectedItemCount, BigDecimal.ZERO, false);
	}

	private CheckoutPreviewResponse emptyPreview() {
		return new CheckoutPreviewResponse(null, PaymentMethod.COD, List.of(), List.of(), null,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), false);
	}
}
