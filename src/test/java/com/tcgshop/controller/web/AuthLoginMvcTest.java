package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.OtpPropertiesConfiguration;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.request.ForgotPasswordRequest;
import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.request.ResetPasswordRequest;
import com.tcgshop.dto.response.AuthTokensResponse;
import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.AccountNotActivatedException;
import com.tcgshop.exception.AuthenticationFailedException;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.AuthService;
import com.tcgshop.service.ProfileService;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.ShopRegistrationService;
import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = {AuthController.class, ProfileController.class, SellerRegistrationController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, OtpPropertiesConfiguration.class, RoleRedirectResolver.class,
		JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class})
class AuthLoginMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean private AuthService authService;
	@MockBean private ProfileService profileService;
	@MockBean private CookieUtil cookieUtil;
	@MockBean private JwtProvider jwtProvider;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private PasswordEncoder passwordEncoder;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private ShopRegistrationService shopRegistrationService;

	@BeforeEach
	void setUp() throws Exception {
		when(cookieUtil.readAccessToken(any())).thenReturn(Optional.empty());
		when(cookieUtil.readRefreshToken(any())).thenReturn(Optional.empty());
		doAnswer(invocation -> {
			var response = invocation.<jakarta.servlet.http.HttpServletResponse>getArgument(0);
			String token = invocation.getArgument(1);
			response.addHeader(HttpHeaders.SET_COOKIE,
					"ACCESS_TOKEN=" + token + "; Path=/; HttpOnly; SameSite=Lax");
			return null;
		}).when(cookieUtil).addAccessTokenCookie(any(), any());
		doAnswer(invocation -> {
			var response = invocation.<jakarta.servlet.http.HttpServletResponse>getArgument(0);
			String token = invocation.getArgument(1);
			response.addHeader(HttpHeaders.SET_COOKIE,
					"REFRESH_TOKEN=" + token + "; Path=/; HttpOnly; SameSite=Lax");
			return null;
		}).when(cookieUtil).addRefreshTokenCookie(any(), any());
		doAnswer(invocation -> {
			var response = invocation.<jakarta.servlet.http.HttpServletResponse>getArgument(0);
			response.addHeader(HttpHeaders.SET_COOKIE,
					"ACCESS_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
			response.addHeader(HttpHeaders.SET_COOKIE,
					"REFRESH_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
			return null;
		}).when(cookieUtil).clearAuthCookies(any());
	}

	@Test
	void getLoginRendersLoginFormAndPreservesReturnUrl() throws Exception {
		mockMvc.perform(get("/auth/login").param("returnUrl", "/cart"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/login"))
				.andExpect(model().attribute("loginRequest", org.hamcrest.Matchers.hasProperty("returnUrl", org.hamcrest.Matchers.is("/cart"))));
	}

	@Test
	void chatbotHistoryKeyUsesGuestOrAuthenticatedEmail() throws Exception {
		mockMvc.perform(get("/auth/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-chat-user=\"guest_chat\"")));

		mockMvc.perform(get("/profile/change-password")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"data-chat-user=\"buyer@example.com\"")));
	}

	@Test
	void successfulLoginSetsHttpOnlyCookiesAndRedirectsByEachRole() throws Exception {
		Object[][] cases = {
				{RoleName.ROLE_ADMIN, "/manager/users"},
				{RoleName.ROLE_MANAGER, "/manager/users"},
				{RoleName.ROLE_SHIPPER, "/shipper/dashboard"},
				{RoleName.ROLE_VENDOR, "/vendor/dashboard"},
				{RoleName.ROLE_USER, "/"}
		};
		for (Object[] item : cases) {
			RoleName role = (RoleName) item[0];
			String target = (String) item[1];
			when(authService.login(any(LoginRequest.class))).thenReturn(
					new AuthTokensResponse("access-" + role, "refresh-" + role, Set.of(role), false));

			mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(target))
				.andExpect(result -> {
					String cookies = String.join(";", result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("ACCESS_TOKEN=access-" + role));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("REFRESH_TOKEN=refresh-" + role));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("HttpOnly"));
				});
		}
	}

	@Test
	void safeReturnUrlWinsAndExternalReturnUrlsAreIgnored() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenReturn(
				new AuthTokensResponse("access", "refresh", Set.of(RoleName.ROLE_ADMIN), false));
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234")
				.param("returnUrl", "/orders/12"))
				.andExpect(redirectedUrl("/orders/12"));

		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234")
				.param("returnUrl", "//evil.com"))
				.andExpect(redirectedUrl("/manager/users"));
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234")
				.param("returnUrl", "http://evil.com"))
				.andExpect(redirectedUrl("/manager/users"));
	}

	@Test
	void authenticatedLoginHonorsSafeReturnUrlAfterPasswordChangeRequirement() throws Exception {
		mockMvc.perform(get("/auth/login").param("returnUrl", "/orders/12")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false))))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders/12"));

		mockMvc.perform(get("/auth/login").param("returnUrl", "/orders/12")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(true))))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
	}

	@Test
	void headerSellerActionReflectsRegistrationStateAndVendorRole() throws Exception {
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.empty());
		mockMvc.perform(get("/seller-registration")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng ký bán hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Trạng thái Shop"))));

		ShopRegistrationResponse pendingShop = new ShopRegistrationResponse(
				9L, "Moonlight Cards", null, null, "0912345678", "Hà Nội", ShopStatus.PENDING, null);
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(pendingShop));
		mockMvc.perform(get("/seller-registration/status")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Trạng thái Shop"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Đăng ký bán hàng"))));

		ShopRegistrationResponse approvedShop = new ShopRegistrationResponse(
				9L, "Moonlight Cards", null, null, "0912345678", "Hà Nội", ShopStatus.APPROVED, null);
		when(shopRegistrationService.getCurrentUserShop()).thenReturn(Optional.of(approvedShop));
		mockMvc.perform(get("/seller-registration/status")
				.with(SecurityMockMvcRequestPostProcessors.user(
						userDetails(false, RoleName.ROLE_USER, RoleName.ROLE_VENDOR))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý shop")));
	}

	@Test
	void failedLoginKeepsEmailButNotPassword() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenThrow(new AuthenticationFailedException());
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "secret")
				.param("returnUrl", "/cart"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/login"))
				.andExpect(model().attribute("errorMessage", "Email hoặc mật khẩu không đúng"))
				.andExpect(model().attribute("loginRequest", org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("buyer@example.com"))))
				.andExpect(model().attribute("loginRequest", org.hamcrest.Matchers.hasProperty("returnUrl", org.hamcrest.Matchers.is("/cart"))))
				.andExpect(model().attribute("loginRequest", org.hamcrest.Matchers.hasProperty("password", org.hamcrest.Matchers.nullValue())));
	}

	@Test
	void inactiveAccountRedirectsToOtpAndMustChangePasswordRedirectsToChangeForm() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenThrow(new AccountNotActivatedException("buyer@example.com"));
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234"))
				.andExpect(redirectedUrl("/auth/verify-otp?email=buyer@example.com"))
				.andExpect(flash().attribute("successMessage", "Tài khoản chưa được kích hoạt, vui lòng nhập mã OTP"));

		when(authService.login(any(LoginRequest.class))).thenReturn(
				new AuthTokensResponse("access", "refresh", Set.of(RoleName.ROLE_USER), true));
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234"))
				.andExpect(redirectedUrl("/profile/change-password"));
	}

	@Test
	void loginPostRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/auth/login").param("email", "buyer@example.com").param("password", "Pass1234"))
				.andExpect(status().isForbidden());
	}

	@Test
	void mustChangePasswordBlocksWebAndApiExceptAllowedRoutes() throws Exception {
		CustomUserDetails user = userDetails(true);
		when(cookieUtil.readAccessToken(any())).thenReturn(Optional.of("valid-jwt"));
		when(jwtProvider.validateToken("valid-jwt")).thenReturn(true);
		when(jwtProvider.getEmail("valid-jwt")).thenReturn("buyer@example.com");
		when(userDetailsService.loadUserByUsername("buyer@example.com")).thenReturn(user);

		mockMvc.perform(get("/vendor/ping").with(SecurityMockMvcRequestPostProcessors.user(user)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
		mockMvc.perform(get("/api/vendor/ping").servletPath("/api/vendor/ping").with(SecurityMockMvcRequestPostProcessors.user(user)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Bạn cần đổi mật khẩu trước khi tiếp tục"))
				.andExpect(jsonPath("$.path").value("/api/vendor/ping"));
		mockMvc.perform(get("/profile/change-password").servletPath("/profile/change-password").with(SecurityMockMvcRequestPostProcessors.user(user)))
				.andExpect(status().isOk()).andExpect(view().name("profile/change-password"));
	}

	@Test
	void logoutIsPostOnlyRevokesRefreshTokenAndClearsBothCookies() throws Exception {
		when(cookieUtil.readRefreshToken(any())).thenReturn(Optional.of("refresh-cookie"));
		when(refreshTokenService.rotate("refresh-cookie")).thenReturn(new RefreshTokenService.Rotation("refresh-cookie", 5L, "buyer@example.com"));
		mockMvc.perform(post("/auth/logout").with(csrf())
				.cookie(new Cookie("REFRESH_TOKEN", "refresh-cookie")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login"))
				.andExpect(flash().attribute("successMessage", "Bạn đã đăng xuất"))
				.andExpect(result -> {
					String cookies = String.join(";", result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("ACCESS_TOKEN=;"));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("REFRESH_TOKEN=;"));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("Max-Age=0"));
				});
		verify(authService).logout("refresh-cookie");
		mockMvc.perform(get("/auth/logout")).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void successfulPasswordChangeSetsFreshCookiesAndRedirectsByRole() throws Exception {
		when(cookieUtil.readAccessToken(any())).thenReturn(Optional.of("valid-jwt"));
		when(jwtProvider.validateToken("valid-jwt")).thenReturn(true);
		when(jwtProvider.getEmail("valid-jwt")).thenReturn("buyer@example.com");
		when(userDetailsService.loadUserByUsername("buyer@example.com")).thenReturn(userDetails(true));
		when(authService.changePassword(any())).thenReturn(
				new AuthTokensResponse("fresh-access", "fresh-refresh", Set.of(RoleName.ROLE_USER), false));

		mockMvc.perform(post("/profile/change-password").with(csrf())
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false)))
				.param("currentPassword", "Current1").param("newPassword", "StrongPass2")
				.param("confirmPassword", "StrongPass2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(result -> {
					String cookies = String.join(";", result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("ACCESS_TOKEN=fresh-access"));
					org.junit.jupiter.api.Assertions.assertTrue(cookies.contains("REFRESH_TOKEN=fresh-refresh"));
				});
	}

	@Test
	void forgotPasswordRejectsMalformedEmailInForm() throws Exception {
		mockMvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "not-an-email"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/forgot-password"))
				.andExpect(model().attributeHasFieldErrors("forgotPasswordRequest", "email"));
	}

	@Test
	void forgotPasswordUsesIdenticalRedirectAndFlashForExistingAndUnknownEmails() throws Exception {
		org.springframework.test.web.servlet.MvcResult knownEmail = mockMvc.perform(validForgotPassword()
				.param("email", "buyer@example.com")).andReturn();
		org.springframework.test.web.servlet.MvcResult unknownEmail = mockMvc.perform(validForgotPassword()
				.param("email", "missing@example.com")).andReturn();

		org.junit.jupiter.api.Assertions.assertEquals(302, knownEmail.getResponse().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(302, unknownEmail.getResponse().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				"/auth/reset-password?email=buyer@example.com",
				knownEmail.getResponse().getRedirectedUrl());
		org.junit.jupiter.api.Assertions.assertEquals(
				"/auth/reset-password?email=missing@example.com",
				unknownEmail.getResponse().getRedirectedUrl());
		org.junit.jupiter.api.Assertions.assertEquals(
				new java.net.URI(knownEmail.getResponse().getRedirectedUrl()).getPath(),
				new java.net.URI(unknownEmail.getResponse().getRedirectedUrl()).getPath());
		org.junit.jupiter.api.Assertions.assertEquals(
				knownEmail.getFlashMap().get("successMessage"), unknownEmail.getFlashMap().get("successMessage"));
		org.junit.jupiter.api.Assertions.assertEquals(
				"Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi. Vui lòng kiểm tra hộp thư (cả thư mục spam)",
				knownEmail.getFlashMap().get("successMessage"));
		verify(authService, org.mockito.Mockito.times(2))
				.requestPasswordReset(org.mockito.ArgumentMatchers.any(ForgotPasswordRequest.class));
	}

	@Test
	void resetPasswordValidationReturnsFormWithoutKeepingOtpOrPasswords() throws Exception {
		mockMvc.perform(validResetPassword().param("email", "buyer@example.com")
				.param("otp", "12345").param("newPassword", "weak")
				.param("confirmPassword", "different"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/reset-password"))
				.andExpect(model().attributeHasFieldErrors("resetPasswordRequest", "otp", "newPassword"))
				.andExpect(model().attributeHasErrors("resetPasswordRequest"))
				.andExpect(result -> {
					ResetPasswordRequest request = (ResetPasswordRequest) result.getModelAndView()
							.getModel().get("resetPasswordRequest");
					org.junit.jupiter.api.Assertions.assertEquals("buyer@example.com", request.getEmail());
					org.junit.jupiter.api.Assertions.assertNull(request.getOtp());
					org.junit.jupiter.api.Assertions.assertNull(request.getNewPassword());
					org.junit.jupiter.api.Assertions.assertNull(request.getConfirmPassword());
				});
		verify(authService, org.mockito.Mockito.never())
				.resetPassword(org.mockito.ArgumentMatchers.any(ResetPasswordRequest.class));
	}

	@Test
	void resetPasswordRejectsPasswordConfirmationMismatch() throws Exception {
		mockMvc.perform(validResetPassword().param("email", "buyer@example.com")
				.param("otp", "001234").param("newPassword", "StrongPass1")
				.param("confirmPassword", "DifferentPass2"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/reset-password"))
				.andExpect(model().attributeHasErrors("resetPasswordRequest"));
	}

	@Test
	void resetPasswordSuccessRedirectsToLoginWithFlashMessage() throws Exception {
		mockMvc.perform(validResetPassword().param("email", "buyer@example.com")
				.param("otp", "001234").param("newPassword", "StrongPass1")
				.param("confirmPassword", "StrongPass1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login"))
				.andExpect(flash().attribute("successMessage", "Đặt lại mật khẩu thành công, vui lòng đăng nhập"));
		verify(authService).resetPassword(org.mockito.ArgumentMatchers.any(ResetPasswordRequest.class));
	}

	@Test
	void resetPasswordBadRequestShowsErrorAndKeepsEmailInRedirect() throws Exception {
		doThrow(new BadRequestException(
				"Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã"))
				.when(authService).resetPassword(org.mockito.ArgumentMatchers.any(ResetPasswordRequest.class));

		mockMvc.perform(validResetPassword()
				.header("Referer", "http://localhost/auth/reset-password?email=buyer@example.com")
				.param("email", "buyer@example.com")
				.param("otp", "001234").param("newPassword", "StrongPass1")
				.param("confirmPassword", "StrongPass1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/reset-password?email=buyer@example.com"))
				.andExpect(flash().attribute("errorMessage",
						"Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã"));
	}

	@Test
	void resetPasswordPostRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/auth/reset-password").param("email", "buyer@example.com")
				.param("otp", "001234").param("newPassword", "StrongPass1")
				.param("confirmPassword", "StrongPass1"))
				.andExpect(status().isForbidden());
	}

	@Test
	void authenticatedUserCannotOpenForgotPasswordForm() throws Exception {
		mockMvc.perform(get("/auth/forgot-password")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails(false))))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
	}

	@Test
	void resetPasswordGetRequiresEmail() throws Exception {
		mockMvc.perform(get("/auth/reset-password"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/forgot-password"));
	}

	private MockHttpServletRequestBuilder validLogin() {
		return post("/auth/login").with(csrf());
	}

	private MockHttpServletRequestBuilder validForgotPassword() {
		return post("/auth/forgot-password").with(csrf());
	}

	private MockHttpServletRequestBuilder validResetPassword() {
		return post("/auth/reset-password").with(csrf());
	}

	private CustomUserDetails userDetails(boolean mustChangePassword) {
		return userDetails(mustChangePassword, RoleName.ROLE_USER);
	}

	private CustomUserDetails userDetails(boolean mustChangePassword, RoleName... roleNames) {
		User user = new User();
		user.setId(5L);
		user.setEmail("buyer@example.com");
		user.setPassword("hash");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(mustChangePassword);
		Set<Role> roles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = new Role();
			role.setName(roleName);
			roles.add(role);
		}
		user.setRoles(roles);
		return new CustomUserDetails(user);
	}
}
