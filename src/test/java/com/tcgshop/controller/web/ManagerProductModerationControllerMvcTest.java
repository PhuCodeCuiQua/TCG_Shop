package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.dto.response.ShopOptionResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
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
import com.tcgshop.service.ProductModerationService;
import com.tcgshop.service.RefreshTokenService;
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

@WebMvcTest(controllers = ManagerProductModerationController.class, properties = {
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
class ManagerProductModerationControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private ProductModerationService productModerationService;
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
	void managerCanRenderListAndDetail() throws Exception {
		when(productModerationService.search(any(), any(), any(), any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));
		when(productModerationService.getShopOptions()).thenReturn(List.of());
		when(productModerationService.getById(15L)).thenReturn(product());

		mockMvc.perform(get("/manager/products").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/products/list"));
		mockMvc.perform(get("/manager/products/15").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/products/detail"));
	}

	@Test
	void userCannotAccessProductModeration() throws Exception {
		mockMvc.perform(get("/manager/products").cookie(userCookie()))
				.andExpect(status().isForbidden());
		verify(productModerationService, never()).search(any(), any(), any(), any(), any());
	}

	@Test
	void invalidBanReasonReturnsDetailWithoutCallingService() throws Exception {
		when(productModerationService.getById(15L)).thenReturn(product());

		mockMvc.perform(post("/manager/products/15/ban")
				.cookie(managerCookie())
				.with(csrf())
				.param("reason", " "))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/products/detail"));

		verify(productModerationService, never()).ban(any(), any());
	}

	@Test
	void validBanUsesPostRedirectGet() throws Exception {
		mockMvc.perform(post("/manager/products/15/ban")
				.cookie(managerCookie())
				.with(csrf())
				.param("reason", "Sản phẩm vi phạm chính sách"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/manager/products/15"));

		verify(productModerationService).ban(any(), any());
	}

	private Cookie managerCookie() {
		return cookie("manager@example.com", RoleName.ROLE_MANAGER);
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

	private ProductModerationResponse product() {
		return new ProductModerationResponse(15L, "Thẻ mẫu", 5L, "Cửa hàng mẫu",
				ProductType.SINGLE, ProductStatus.ACTIVE, 3, null, LocalDateTime.now());
	}
}
