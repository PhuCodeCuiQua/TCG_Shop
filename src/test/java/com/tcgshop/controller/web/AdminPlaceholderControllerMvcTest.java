package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AdminPlaceholderController.class, DashboardPageController.class, AdminSettingsController.class, StaticPageController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class AdminPlaceholderControllerMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockBean
	private UserDetailsServiceImpl userDetailsService;

	@MockBean
	private RefreshTokenService refreshTokenService;
	@MockBean private com.tcgshop.service.SystemSettingService systemSettingService;
	@MockBean private com.tcgshop.mapper.SystemSettingMapper settingsMapper;

	@BeforeEach
	void configureUsers() {
		when(systemSettingService.getSettings()).thenReturn(new com.tcgshop.dto.response.SystemSettingResponse(
				"Rare Market", "https://res.cloudinary.com/demo/logo.png", "0901234567", "support@example.com"));
		when(settingsMapper.toRequest(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
				org.mapstruct.factory.Mappers.getMapper(com.tcgshop.mapper.SystemSettingMapper.class)
						.toRequest(invocation.getArgument(0)));
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			String role = email.substring(0, email.indexOf('@')).toUpperCase();
			return details(RoleName.valueOf(role));
		});
	}

	@Test
	void publicHeaderAndFooterUseFreshBrandWithFallbackWhenNoSettingsExist() throws Exception {
		mockMvc.perform(get("/privacy")).andExpect(status().isOk())
				.andExpect(content().string(containsString("Rare Market")))
				.andExpect(content().string(containsString("https://res.cloudinary.com/demo/logo.png")));
		when(systemSettingService.getSettings()).thenReturn(com.tcgshop.dto.response.SystemSettingResponse.defaults());
		mockMvc.perform(get("/privacy")).andExpect(status().isOk())
				.andExpect(content().string(containsString("TCGSHOP")))
				.andExpect(content().string(containsString("/img/default-logo.svg")));
	}

	@Test
	void uploadErrorKeepsFormValuesAndDisplaysFieldError() throws Exception {
		org.mockito.Mockito.doThrow(new com.tcgshop.exception.BadRequestException("Ảnh không hợp lệ"))
				.when(systemSettingService).save(org.mockito.ArgumentMatchers.any());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/settings")
				.cookie(accessCookie(RoleName.ROLE_ADMIN)).param("siteName", "My market")
				.param("contactPhone", "0901234567").param("contactEmail", "help@example.com")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("settingsRequest", "logo"))
				.andExpect(content().string(containsString("My market")))
				.andExpect(content().string(containsString("Ảnh không hợp lệ")));
	}

	@Test
	void settingsPrefillAndBrandAppearInDashboard() throws Exception {
		mockMvc.perform(get("/admin/settings").cookie(accessCookie(RoleName.ROLE_ADMIN)))
				.andExpect(status().isOk()).andExpect(content().string(containsString("Rare Market")))
				.andExpect(content().string(containsString("https://res.cloudinary.com/demo/logo.png")))
				.andExpect(content().string(containsString("support@example.com")));
	}

	@Test
	void settingsPostValidatesAndRequiresAdminAndCsrf() throws Exception {
		var valid = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/settings")
				.param("siteName", "New name").param("contactPhone", "0901234567").param("contactEmail", "help@example.com");
		mockMvc.perform(valid.cookie(accessCookie(RoleName.ROLE_ADMIN))
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/settings"));
		org.mockito.Mockito.verify(systemSettingService).save(org.mockito.ArgumentMatchers.any());
		org.mockito.Mockito.clearInvocations(systemSettingService);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/settings")
				.cookie(accessCookie(RoleName.ROLE_ADMIN)).param("siteName", " ").param("contactEmail", "invalid")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("settingsRequest", "siteName", "contactPhone", "contactEmail"));
		org.mockito.Mockito.verify(systemSettingService, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/settings")
				.cookie(accessCookie(RoleName.ROLE_ADMIN))).andExpect(status().isForbidden());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/settings")
				.cookie(accessCookie(RoleName.ROLE_MANAGER)).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminToolsAndRemainingPlaceholderRenderThroughDashboardLayout() throws Exception {
		assertPlaceholder("/manager/orders", "manager/orders/list");
		for (String page : new String[] {"roles", "settings", "logs"}) {
			mockMvc.perform(get("/admin/" + page).cookie(accessCookie(RoleName.ROLE_ADMIN)))
					.andExpect(status().isOk()).andExpect(view().name("admin/" + page + "/index"))
					.andExpect(content().string(containsString("admin-tools")))
					.andExpect(content().string(containsString("dashboardAccountMenu")))
					.andExpect(content().string(not(containsString("Trang này đang được phát triển"))));
		}
	}

	@Test
	void managerCanOpenSharedOrdersPlaceholderButCannotOpenAdminOnlyPlaceholders() throws Exception {
		mockMvc.perform(get("/manager/orders").cookie(accessCookie(RoleName.ROLE_MANAGER)))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/orders/list"));

		for (String page : new String[] {"roles", "settings", "logs"}) {
			mockMvc.perform(get("/admin/" + page).cookie(accessCookie(RoleName.ROLE_MANAGER)))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void adminDashboardUsesTheSharedDashboardLayout() throws Exception {
		mockMvc.perform(get("/admin/dashboard").cookie(accessCookie(RoleName.ROLE_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/dashboard-stats"))
				.andExpect(content().string(containsString("dashboardSidebarMobile")))
				.andExpect(content().string(containsString("href=\"/manager/orders\"")))
				.andExpect(content().string(containsString("href=\"/admin/roles\"")))
				.andExpect(content().string(containsString("href=\"/admin/settings\"")))
				.andExpect(content().string(containsString("href=\"/admin/logs\"")))
				.andExpect(content().string(not(containsString("js-coming-soon"))));
	}

	private void assertPlaceholder(String path, String viewName) throws Exception {
		mockMvc.perform(get(path).cookie(accessCookie(RoleName.ROLE_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(view().name(viewName))
				.andExpect(content().string(containsString("Trang này đang được phát triển")));
	}

	private Cookie accessCookie(RoleName roleName) {
		return new Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(details(roleName)));
	}

	private CustomUserDetails details(RoleName roleName) {
		User user = new User();
		user.setId(42L);
		user.setEmail(roleName.name().toLowerCase() + "@example.com");
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		Role role = new Role();
		role.setName(roleName);
		user.setRoles(Set.of(role));
		return new CustomUserDetails(user);
	}
}
