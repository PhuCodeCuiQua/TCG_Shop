package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.StaffAccountService;
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

@WebMvcTest(controllers = {
		AdminManagerAccountController.class,
		ManagerShipperAccountController.class
}, properties = {
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
class StaffAccountControllerMvcTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private StaffAccountService staffAccountService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void configureUsers() {
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			RoleName role = email.startsWith("admin") ? RoleName.ROLE_ADMIN : RoleName.ROLE_MANAGER;
			return details(email, role);
		});
	}

	@Test
	void adminCanRenderManagerCreationForm() throws Exception {
		mockMvc.perform(get("/admin/managers/new").cookie(adminCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/managers/form"));
	}

	@Test
	void managerCannotRenderAdminOnlyManagerCreationForm() throws Exception {
		mockMvc.perform(get("/admin/managers/new").cookie(managerCookie()))
				.andExpect(status().isForbidden());
	}

	@Test
	void managerCanRenderShipperListAndCreationForm() throws Exception {
		when(staffAccountService.searchShippers(any(), any(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

		mockMvc.perform(get("/manager/shippers").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shippers/list"));
		mockMvc.perform(get("/manager/shippers/new").cookie(managerCookie()))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shippers/form"));
	}

	@Test
	void invalidShipperRequestReturnsFormWithoutCallingService() throws Exception {
		mockMvc.perform(post("/manager/shippers")
				.cookie(managerCookie())
				.with(csrf())
				.param("fullName", "")
				.param("email", "invalid")
				.param("phoneNumber", "123"))
				.andExpect(status().isOk())
				.andExpect(view().name("manager/shippers/form"));

		verify(staffAccountService, never()).createShipper(any());
	}

	private Cookie adminCookie() {
		return new Cookie("ACCESS_TOKEN",
				jwtProvider.generateAccessToken(details("admin@example.com", RoleName.ROLE_ADMIN)));
	}

	private Cookie managerCookie() {
		return new Cookie("ACCESS_TOKEN",
				jwtProvider.generateAccessToken(details("manager@example.com", RoleName.ROLE_MANAGER)));
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
