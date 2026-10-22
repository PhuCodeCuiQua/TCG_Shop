package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.response.AuthTokensResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.AccountNotActivatedException;
import com.tcgshop.exception.AuthenticationFailedException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.AuthService;
import com.tcgshop.service.RefreshTokenService;
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

@WebMvcTest(controllers = {AuthController.class, ProfileController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, OtpPropertiesConfiguration.class, RoleRedirectResolver.class,
		JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class})
class AuthLoginMvcTest {
	private static final String CSRF = "test-csrf-token";

	@Autowired
	private MockMvc mockMvc;

	@MockBean private AuthService authService;
	@MockBean private CookieUtil cookieUtil;
	@MockBean private JwtProvider jwtProvider;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private PasswordEncoder passwordEncoder;
	@MockBean private RefreshTokenService refreshTokenService;

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
	void successfulLoginSetsHttpOnlyCookiesAndRedirectsByEachRole() throws Exception {
		Object[][] cases = {
				{RoleName.ROLE_ADMIN, "/admin/dashboard"},
				{RoleName.ROLE_MANAGER, "/manager/dashboard"},
				{RoleName.ROLE_SHIPPER, "/shipper/orders"},
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
				.andExpect(redirectedUrl("/admin/dashboard"));
		mockMvc.perform(validLogin().param("email", "buyer@example.com").param("password", "Pass1234")
				.param("returnUrl", "http://evil.com"))
				.andExpect(redirectedUrl("/admin/dashboard"));
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
				.andExpect(redirectedUrl("/auth/verify-otp?email=buyer%40example.com"))
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

		mockMvc.perform(get("/vendor/ping"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
		mockMvc.perform(get("/api/vendor/ping"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Bạn cần đổi mật khẩu trước khi tiếp tục"))
				.andExpect(jsonPath("$.path").value("/api/vendor/ping"));
		mockMvc.perform(get("/profile/change-password"))
				.andExpect(status().isOk()).andExpect(view().name("profile/change-password"));
	}

	@Test
	void logoutIsPostOnlyRevokesRefreshTokenAndClearsBothCookies() throws Exception {
		when(cookieUtil.readRefreshToken(any())).thenReturn(Optional.of("refresh-cookie"));
		mockMvc.perform(post("/auth/logout").cookie(new Cookie("REFRESH_TOKEN", "refresh-cookie"))
				.cookie(new Cookie("XSRF-TOKEN", CSRF)).header("X-XSRF-TOKEN", CSRF))
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

		mockMvc.perform(post("/profile/change-password")
				.cookie(new Cookie("XSRF-TOKEN", CSRF)).header("X-XSRF-TOKEN", CSRF)
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

	private MockHttpServletRequestBuilder validLogin() {
		return post("/auth/login").cookie(new Cookie("XSRF-TOKEN", CSRF)).header("X-XSRF-TOKEN", CSRF);
	}

	private CustomUserDetails userDetails(boolean mustChangePassword) {
		User user = new User();
		user.setId(5L);
		user.setEmail("buyer@example.com");
		user.setPassword("hash");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(mustChangePassword);
		Role role = new Role();
		role.setName(RoleName.ROLE_USER);
		user.setRoles(new HashSet<>(Set.of(role)));
		return new CustomUserDetails(user);
	}
}
