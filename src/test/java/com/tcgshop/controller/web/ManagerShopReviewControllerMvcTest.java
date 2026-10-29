package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.request.ShopProfileUpdateRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShopReviewResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.ShopReviewService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ManagerShopReviewController.class, properties = {
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
class ManagerShopReviewControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private ShopReviewService shopReviewService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void configureUsers() {
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			RoleName role = email.startsWith("admin")
					? RoleName.ROLE_ADMIN
					: email.startsWith("manager") ? RoleName.ROLE_MANAGER : RoleName.ROLE_USER;
			return details(email, role);
		});
	}

	@Test
	void managerCanRenderShopListAndDetail() throws Exception {
		when(shopReviewService.search(any(), any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));
		when(shopReviewService.getById(12L)).thenReturn(shop(ShopStatus.PENDING));

		mockMvc.perform(get("/manager/shops").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/list"));
		mockMvc.perform(get("/manager/shops/12").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/detail"));
		when(shopReviewService.getById(12L)).thenReturn(shop(ShopStatus.PENDING));
		mockMvc.perform(get("/manager/shops/12/edit").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/form"));
	}

	@Test
	void adminCanAccessManagerShopReviewPages() throws Exception {
		when(shopReviewService.search(any(), any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

		mockMvc.perform(get("/manager/shops").cookie(adminCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/list"));
	}

	@Test
	void userCannotAccessShopReviewPages() throws Exception {
		mockMvc.perform(get("/manager/shops").cookie(userCookie()))
				.andExpect(status().isForbidden());

		verify(shopReviewService, never()).search(any(), any(), any());
	}

	@Test
	void invalidRejectRequestReturnsDetailAndDoesNotCallService() throws Exception {
		when(shopReviewService.getById(12L)).thenReturn(shop(ShopStatus.PENDING));

		mockMvc.perform(post("/manager/shops/12/reject")
				.cookie(managerCookie())
				.with(csrf())
				.param("reason", " "))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/detail"));

		verify(shopReviewService, never()).reject(eq(12L), any());
	}

	@Test
	void validRejectRequestUsesPostRedirectGet() throws Exception {
		mockMvc.perform(post("/manager/shops/12/reject")
				.cookie(managerCookie())
				.with(csrf())
				.param("reason", "Thiếu thông tin hồ sơ"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.redirectedUrl("/manager/shops/12"));

		verify(shopReviewService).reject(eq(12L), any());
	}

	@Test
	void invalidShopProfileUpdateReturnsFormWithoutCallingService() throws Exception {
		when(shopReviewService.getById(12L)).thenReturn(shop(ShopStatus.PENDING));

		mockMvc.perform(post("/manager/shops/12/edit")
				.cookie(managerCookie())
				.with(csrf())
				.param("name", "")
				.param("phoneNumber", "invalid")
				.param("pickupAddress", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shops/form"));

		verify(shopReviewService, never()).updateProfile(eq(12L), any());
	}

	@Test
	void validShopProfileUpdateUsesPostRedirectGet() throws Exception {
		when(shopReviewService.updateProfile(eq(12L), any(ShopProfileUpdateRequest.class)))
				.thenReturn(shop(ShopStatus.PENDING));

		mockMvc.perform(post("/manager/shops/12/edit")
				.cookie(managerCookie())
				.with(csrf())
				.param("name", "Card Shop Updated")
				.param("phoneNumber", "0912345678")
				.param("pickupAddress", "Địa chỉ mới")
				.param("description", "Mô tả mới"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.redirectedUrl("/manager/shops/12"));

		verify(shopReviewService).updateProfile(eq(12L), any(ShopProfileUpdateRequest.class));
	}

	private Cookie managerCookie() {
		return cookie("manager@example.com", RoleName.ROLE_MANAGER);
	}

	private Cookie adminCookie() {
		return cookie("admin@example.com", RoleName.ROLE_ADMIN);
	}

	private Cookie userCookie() {
		return cookie("user@example.com", RoleName.ROLE_USER);
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

	private ShopReviewResponse shop(ShopStatus status) {
		return new ShopReviewResponse(12L, "Card Shop", "card-shop", null, null,
				"0900000000", "Địa chỉ lấy hàng", "Mô tả cửa hàng",
				status, null, 22L, "owner@example.com", "Chủ cửa hàng", LocalDateTime.now());
	}
}
