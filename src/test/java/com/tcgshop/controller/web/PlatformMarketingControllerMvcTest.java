package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.PlatformCouponAdminService;
import com.tcgshop.service.PlatformPromotionService;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PlatformPromotionController.class, PlatformCouponAdminController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class,
		CurrentUser.class})
class PlatformMarketingControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private PlatformPromotionService promotionService;
	@MockBean private PlatformCouponAdminService couponAdminService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void configureUsers() {
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			return details(email, email.startsWith("admin") ? RoleName.ROLE_ADMIN : RoleName.ROLE_MANAGER);
		});
		when(promotionService.search(any(), any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 15, 0, 0, true, true));
		when(couponAdminService.search(any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 15, 0, 0, true, true));
		when(promotionService.getGameOptions()).thenReturn(List.of());
		when(promotionService.getSeriesOptions()).thenReturn(List.of());
		when(promotionService.getProductOptions()).thenReturn(List.of());
	}

	@Test
	void rendersPromotionAdminFormAndManagerListing() throws Exception {
		mockMvc.perform(get("/admin/promotions/new").cookie(adminCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/promotions/form"));

		mockMvc.perform(get("/manager/promotions").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/promotions/list"));
	}

	@Test
	void rendersPlatformCouponAdminFormAndManagerListing() throws Exception {
		mockMvc.perform(get("/admin/platform-coupons/new").cookie(adminCookie()))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
						org.hamcrest.Matchers.matchesPattern("(?s).*<input[^>]*name=\"_csrf\"[^>]*value=\"[^\"]+\"[^>]*>.*")))
				.andExpect(view().name("manager/platform-coupons/form"));

		mockMvc.perform(get("/manager/platform-coupons").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/platform-coupons/list"));
	}

	private Cookie adminCookie() {
		return cookie("admin@example.com", RoleName.ROLE_ADMIN);
	}

	private Cookie managerCookie() {
		return cookie("manager@example.com", RoleName.ROLE_MANAGER);
	}

	private Cookie cookie(String email, RoleName role) {
		return new Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(details(email, role)));
	}

	private CustomUserDetails details(String email, RoleName roleName) {
		User user = new User();
		user.setId(42L);
		user.setEmail(email);
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		user.setRoles(EnumSet.of(roleName).stream().map(name -> {
			Role role = new Role();
			role.setName(name);
			return role;
		}).collect(java.util.stream.Collectors.toSet()));
		return new CustomUserDetails(user);
	}
}
