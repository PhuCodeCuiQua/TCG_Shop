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
import com.tcgshop.dto.response.CardPriceOptionResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PriceHistoryResponse;
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
import com.tcgshop.service.PriceHistoryService;
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

@WebMvcTest(controllers = ManagerPriceHistoryController.class, properties = {
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
class ManagerPriceHistoryControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private PriceHistoryService priceHistoryService;
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
	void managerCanViewHistoryManagementPage() throws Exception {
		when(priceHistoryService.search(any(), any(), any(), any()))
				.thenReturn(emptyPage());
		when(priceHistoryService.getActiveCards()).thenReturn(List.of(
				new CardPriceOptionResponse(7L, "Pikachu", "025", "SV1")));

		mockMvc.perform(get("/manager/price-history")
				.cookie(managerCookie())
				.param("cardId", "7"))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/price-history/list"));
	}

	@Test
	void invalidPriceReturnsFormAndDoesNotCallService() throws Exception {
		when(priceHistoryService.search(any(), any(), any(), any())).thenReturn(emptyPage());
		when(priceHistoryService.getActiveCards()).thenReturn(List.of());

		mockMvc.perform(post("/manager/price-history")
				.cookie(managerCookie())
				.with(csrf())
				.param("cardId", "7")
				.param("recordedDate", "2026-10-23")
				.param("marketPrice", "0")
				.param("avgPrice", "100"))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/price-history/list"));

		verify(priceHistoryService, never()).save(any());
	}

	@Test
	void validSaveUsesPostRedirectGetAndKeepsHistoryFilter() throws Exception {
		mockMvc.perform(post("/manager/price-history")
				.cookie(managerCookie())
				.with(csrf())
				.param("cardId", "7")
				.param("recordedDate", "2026-10-23")
				.param("marketPrice", "150.00")
				.param("avgPrice", "140.00"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/manager/price-history?cardId=7&startDate=2026-10-23&endDate=2026-10-23"));

		verify(priceHistoryService).save(any());
	}

	@Test
	void userCannotAccessManagerHistoryPage() throws Exception {
		mockMvc.perform(get("/manager/price-history").cookie(userCookie()))
				.andExpect(status().isForbidden());

		verify(priceHistoryService, never()).search(any(), any(), any(), any());
	}

	private PageResponse<PriceHistoryResponse> emptyPage() {
		return new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
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
}
