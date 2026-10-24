package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.dto.response.ProfileResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.GlobalExceptionHandler;
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
import jakarta.servlet.http.Cookie;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfileController.class, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, RoleRedirectResolver.class,
		GlobalExceptionHandler.class})
class ProfileControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@MockBean private ProfileService profileService;
	@MockBean private AuthService authService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private PasswordEncoder passwordEncoder;
	@MockBean private CookieUtil cookieUtil;

	@BeforeEach
	void setup() {
		when(cookieUtil.readAccessToken(any())).thenReturn(java.util.Optional.empty());
		when(cookieUtil.readRefreshToken(any())).thenReturn(java.util.Optional.empty());
		when(userDetailsService.loadUserByUsername(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> details(invocation.getArgument(0), false, roleFor(invocation.getArgument(0))));
	}

	@Test
	void anonymousRedirectsAndAllAuthenticatedRolesMayViewProfile() throws Exception {
		mockMvc.perform(get("/profile")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login?returnUrl=%2Fprofile"));
		when(profileService.getProfile()).thenReturn(profile());
		for (String email : List.of("user@example.com", "shipper@example.com", "manager@example.com")) {
			mockMvc.perform(get("/profile").with(SecurityMockMvcRequestPostProcessors.user(
					details(email, false, roleFor(email)))))
					.andExpect(status().isOk()).andExpect(view().name("profile/index"))
					.andExpect(model().attribute("profile", profile()));
		}
	}

	@Test
	void invalidProfileFieldsReturnFormErrorsWithoutCallingService() throws Exception {
		when(profileService.getProfile()).thenReturn(profile());
		mockMvc.perform(post("/profile").with(csrf()).with(SecurityMockMvcRequestPostProcessors.user(
				details("user@example.com", false, RoleName.ROLE_USER)))
				.param("fullName", "Alice3").param("phoneNumber", "123"))
				.andExpect(status().isOk()).andExpect(view().name("profile/index"))
				.andExpect(model().attributeHasFieldErrors("form", "fullName", "phoneNumber"))
				.andExpect(model().attribute("profile", profile()));
		verify(profileService, never()).updateProfile(any(UpdateProfileRequest.class));
	}

	@Test
	void extraSensitiveParametersAreIgnoredByProfileFormBinding() throws Exception {
		when(profileService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(profile());
		mockMvc.perform(post("/profile").with(csrf()).with(SecurityMockMvcRequestPostProcessors.user(
				details("user@example.com", false, RoleName.ROLE_USER)))
				.param("fullName", "Alice Nguyen").param("phoneNumber", "0912345678")
				.param("email", "attacker@example.com").param("status", "BLOCKED").param("roles", "ROLE_ADMIN"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile"))
				.andExpect(flash().attribute("successMessage", "Đã cập nhật thông tin"));
		verify(profileService).updateProfile(org.mockito.ArgumentMatchers.argThat(request ->
				"Alice Nguyen".equals(request.getFullName()) && "0912345678".equals(request.getPhoneNumber())));
	}

	@Test
	void avatarUploadRequiresSelectionAndCsrf() throws Exception {
		when(profileService.updateAvatar(any())).thenThrow(new BadRequestException("Vui lòng chọn ảnh"));
		MockMultipartFile empty = new MockMultipartFile("avatar", "", "application/octet-stream", new byte[0]);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/profile/avatar")
				.file(empty).with(csrf()).with(SecurityMockMvcRequestPostProcessors.user(
					details("user@example.com", false, RoleName.ROLE_USER)))
				.header(HttpHeaders.REFERER, "http://localhost/profile"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile"))
				.andExpect(flash().attribute("errorMessage", "Vui lòng chọn ảnh"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/profile/avatar")
				.file(empty).with(SecurityMockMvcRequestPostProcessors.user(
					details("user@example.com", false, RoleName.ROLE_USER))))
				.andExpect(status().isForbidden());
	}

	@Test
	void mustChangePasswordUserIsRedirectedBeforeProfileIsLoaded() throws Exception {
		when(userDetailsService.loadUserByUsername("user@example.com"))
				.thenReturn(details("user@example.com", true, RoleName.ROLE_USER));
		mockMvc.perform(get("/profile").with(SecurityMockMvcRequestPostProcessors.user(
				details("user@example.com", true, RoleName.ROLE_USER))))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile/change-password"));
		verify(profileService, never()).getProfile();
	}

	private RoleName roleFor(String email) {
		if (email.startsWith("shipper")) return RoleName.ROLE_SHIPPER;
		if (email.startsWith("manager")) return RoleName.ROLE_MANAGER;
		return RoleName.ROLE_USER;
	}

	private CustomUserDetails details(String email, boolean mustChangePassword, RoleName roleName) {
		User user = new User();
		user.setId(1L);
		user.setEmail(email);
		user.setFullName("Alice Nguyen");
		user.setPassword("encoded");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(mustChangePassword);
		Role role = new Role();
		role.setName(roleName);
		user.setRoles(new java.util.HashSet<>(Set.of(role)));
		return new CustomUserDetails(user);
	}

	private ProfileResponse profile() {
		return new ProfileResponse("user@example.com", "Alice Nguyen", "0912345678", null,
				EnumSet.of(RoleName.ROLE_USER), null);
	}
}
