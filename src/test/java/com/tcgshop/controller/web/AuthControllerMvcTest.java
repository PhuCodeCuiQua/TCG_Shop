package com.tcgshop.controller.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import com.tcgshop.config.OtpProperties;
import com.tcgshop.config.OtpPropertiesConfiguration;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.service.AuthService;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OtpPropertiesConfiguration.class)
class AuthControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;

	@MockBean
	private CookieUtil cookieUtil;

	@MockBean
	private RoleRedirectResolver roleRedirectResolver;

	@Test
	void logoutRevokesRefreshTokenClearsCookiesAndRedirectsToLogin() throws Exception {
		when(cookieUtil.readRefreshToken(any())).thenReturn(java.util.Optional.of("refresh-token-to-revoke"));
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login"));
		verify(authService).logout("refresh-token-to-revoke");
		verify(cookieUtil).clearAuthCookies(any());
	}

	@Test
	void weakPasswordReturnsRegistrationFormWithoutEchoingPassword() throws Exception {
		mockMvc.perform(post("/auth/register").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"))
				.param("fullName", "Buyer")
				.param("email", "buyer@example.com")
				.param("phoneNumber", "0912345678")
				.param("password", "weak")
				.param("confirmPassword", "weak"))
			.andExpect(view().name("auth/register"))
			.andExpect(model().attributeHasFieldErrors("registerRequest", "password"))
			.andExpect(result -> {
				BindingResult bindingResult = (BindingResult) result.getModelAndView().getModel()
						.get(BindingResult.MODEL_KEY_PREFIX + "registerRequest");
				FieldError passwordError = bindingResult.getFieldError("password");
				org.junit.jupiter.api.Assertions.assertNotNull(passwordError);
				org.junit.jupiter.api.Assertions.assertNull(passwordError.getRejectedValue());
			});
	}

	@Test
	void passwordConfirmationMismatchReturnsRegistrationForm() throws Exception {
		mockMvc.perform(post("/auth/register").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"))
				.param("fullName", "Buyer")
				.param("email", "buyer@example.com")
				.param("phoneNumber", "0912345678")
				.param("password", "StrongPass1")
				.param("confirmPassword", "Different1"))
			.andExpect(view().name("auth/register"))
			.andExpect(model().attributeHasErrors("registerRequest"));
	}

	@Test
	void invalidVietnamesePhoneNumberReturnsRegistrationForm() throws Exception {
		mockMvc.perform(post("/auth/register").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"))
				.param("fullName", "Buyer")
				.param("email", "buyer@example.com")
				.param("phoneNumber", "12345")
				.param("password", "StrongPass1")
				.param("confirmPassword", "StrongPass1"))
			.andExpect(view().name("auth/register"))
			.andExpect(model().attributeHasFieldErrors("registerRequest", "phoneNumber"));
	}

	@Test
	void malformedEmailReturnsRegistrationForm() throws Exception {
		mockMvc.perform(post("/auth/register").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"))
				.param("fullName", "Buyer")
				.param("email", "not-an-email")
				.param("phoneNumber", "0912345678")
				.param("password", "StrongPass1")
				.param("confirmPassword", "StrongPass1"))
			.andExpect(view().name("auth/register"))
			.andExpect(model().attributeHasFieldErrors("registerRequest", "email"));
	}
}
