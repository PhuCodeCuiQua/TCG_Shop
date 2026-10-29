package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.OtpPropertiesConfiguration;
import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.request.AddressRequest;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.RoleRedirectResolver;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.AddressService;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AddressController.class, com.tcgshop.controller.rest.AddressRestController.class},
		properties = {"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
				"app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
				"app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
				"app.cookie.secure=false", "app.address.max-per-user=10"})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, RoleRedirectResolver.class,
		GlobalExceptionHandler.class, OtpPropertiesConfiguration.class})
class AddressControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@MockBean private AddressService addressService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private CookieUtil cookieUtil;

	@BeforeEach
	void setup() {
		when(cookieUtil.readAccessToken(any())).thenReturn(java.util.Optional.empty());
		when(cookieUtil.readRefreshToken(any())).thenReturn(java.util.Optional.empty());
		when(userDetailsService.loadUserByUsername(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> details(false));
		when(addressService.list()).thenReturn(List.of());
	}

	@Test
	void anonymousIsRedirectedAndAuthenticatedUserCanViewAddresses() throws Exception {
		mockMvc.perform(get("/profile/addresses"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login?returnUrl=%2Fprofile%2Faddresses"));
		mockMvc.perform(get("/profile/addresses").with(user(details(false))))
				.andExpect(status().isOk()).andExpect(view().name("profile/addresses/index"))
				.andExpect(model().attribute("addresses", List.of()))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"list-group-item list-group-item-action active\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current=\"page\"")));
	}

	@Test
	void invalidAddressFieldsReturnFormAndDoNotCallCreate() throws Exception {
		mockMvc.perform(post("/profile/addresses").with(user(details(false))).with(csrf())
				.param("receiverName", "Nguyen3")
				.param("phoneNumber", "123").param("province", "").param("district", "Quan 1")
				.param("ward", "Phuong 1").param("detailAddress", "So 1"))
				.andExpect(status().isOk()).andExpect(view().name("profile/addresses/form"))
				.andExpect(model().attributeHasFieldErrors("form", "receiverName", "phoneNumber", "province"));
		verify(addressService, never()).create(any(AddressRequest.class));
	}

	@Test
	void createUsesOnlyRequestFieldsAndRedirectsToSafeReturnUrl() throws Exception {
		when(addressService.create(any(AddressRequest.class))).thenReturn(address(8L, false));
		mockMvc.perform(post("/profile/addresses").with(user(details(false))).with(csrf())
				.param("receiverName", "Nguyen Van An")
				.param("phoneNumber", "0912345678").param("province", "Hanoi").param("district", "Cau Giay")
				.param("ward", "Dich Vong").param("detailAddress", "So 1")
				.param("returnUrl", "/checkout").param("userId", "999").param("id", "123")
				.param("isDefault", "true"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/checkout"));
		verify(addressService).create(org.mockito.ArgumentMatchers.argThat(request ->
				"Nguyen Van An".equals(request.getReceiverName())
						&& "0912345678".equals(request.getPhoneNumber())
						&& !request.isMakeDefault()));
	}

	@Test
	void unsafeReturnUrlsAreIgnoredAndPostRequiresCsrf() throws Exception {
		for (String returnUrl : List.of("//evil.com", "http://evil.com")) {
			mockMvc.perform(post("/profile/addresses").with(user(details(false))).with(csrf())
					.param("receiverName", "Nguyen Van An")
					.param("phoneNumber", "0912345678").param("province", "Hanoi")
					.param("district", "Cau Giay").param("ward", "Dich Vong")
					.param("detailAddress", "So 1").param("returnUrl", returnUrl))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/profile/addresses"));
		}
		mockMvc.perform(post("/profile/addresses").with(user(details(false))
				).param("receiverName", "Nguyen Van An").param("phoneNumber", "0912345678")
				.param("province", "Hanoi").param("district", "Cau Giay")
				.param("ward", "Dich Vong").param("detailAddress", "So 1"))
				.andExpect(status().isForbidden());
	}

	@Test
	void editMissingAddressUses404PageAndMustChangePasswordRedirects() throws Exception {
		when(addressService.get(999L)).thenThrow(new ResourceNotFoundException("Địa chỉ", 999L));
		mockMvc.perform(get("/profile/addresses/999/edit").with(user(details(false))))
				.andExpect(status().isNotFound()).andExpect(view().name("error/404"));
		mockMvc.perform(get("/profile/addresses").with(user(details(true))))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile/change-password"));
	}

	@Test
	void anonymousAddressApiReturnsJson401() throws Exception {
		mockMvc.perform(get("/api/profile/addresses").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/profile/addresses"));
	}

	private CustomUserDetails details(boolean mustChangePassword) {
		User user = new User();
		user.setId(42L);
		user.setEmail("user@example.com");
		user.setFullName("Nguyen Van An");
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(mustChangePassword);
		Role role = new Role();
		role.setName(RoleName.ROLE_USER);
		user.setRoles(new java.util.HashSet<>(Set.of(role)));
		return new CustomUserDetails(user);
	}

	private AddressResponse address(Long id, boolean isDefault) {
		return new AddressResponse(id, "Nguyen Van An", "0912345678", "Hanoi", "Cau Giay",
				"Dich Vong", "So 1", "So 1, Dich Vong, Cau Giay, Hanoi", isDefault, LocalDateTime.now());
	}
}
