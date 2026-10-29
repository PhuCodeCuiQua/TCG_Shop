package com.tcgshop.controller.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.ReconciliationCheckSummaryResponse;
import com.tcgshop.dto.response.ReconciliationFindingResponse;
import com.tcgshop.dto.response.ReconciliationReportResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ReconciliationSeverity;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.OrderReconciliationService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminReconciliationRestController.class, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class AdminReconciliationRestControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private OrderReconciliationService reconciliationService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void configurePrincipals() {
		when(userDetailsService.loadUserByUsername(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			return switch (email) {
				case "admin@example.test" -> details(email, RoleName.ROLE_ADMIN);
				case "user@example.test" -> details(email, RoleName.ROLE_USER);
				case "manager@example.test" -> details(email, RoleName.ROLE_MANAGER);
			case "shipper@example.test" -> details(email, RoleName.ROLE_SHIPPER);
				default -> throw new UsernameNotFoundException("not found");
			};
		});
	}

	@Test
	void anonymousGetsJson401() throws Exception {
		mockMvc.perform(get("/api/admin/reconciliation"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
		verifyNoInteractions(reconciliationService);
	}

	@Test
	void nonAdminRolesGet403() throws Exception {
		for (String role : List.of("user", "manager", "shipper")) {
			mockMvc.perform(get("/api/admin/reconciliation").cookie(cookie(role + "@example.test", role(role))))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403));
		}
		verifyNoInteractions(reconciliationService);
	}

	@Test
	void adminGetsReportJson() throws Exception {
		ReconciliationReportResponse report = new ReconciliationReportResponse(
				LocalDateTime.of(2026, 10, 24, 12, 0), 3, 1, 1,
				List.of(new ReconciliationCheckSummaryResponse("ORD_NEGATIVE", "Invalid order amount",
						ReconciliationSeverity.ERROR, 1)),
				List.of(new ReconciliationFindingResponse("ORD_NEGATIVE", ReconciliationSeverity.ERROR,
						"ORDER", 7L, "it-order", "negative amount")));
		when(reconciliationService.reconcile()).thenReturn(report);

		mockMvc.perform(get("/api/admin/reconciliation").cookie(cookie("admin@example.test", RoleName.ROLE_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generatedAt").exists())
				.andExpect(jsonPath("$.ordersChecked").value(3))
				.andExpect(jsonPath("$.errorCount").value(1))
				.andExpect(jsonPath("$.warningCount").value(1))
				.andExpect(jsonPath("$.checks[0].code").value("ORD_NEGATIVE"))
				.andExpect(jsonPath("$.checks[0].title").value("Invalid order amount"))
				.andExpect(jsonPath("$.checks[0].severity").value("ERROR"))
				.andExpect(jsonPath("$.checks[0].violationCount").value(1))
				.andExpect(jsonPath("$.findings[0].checkCode").value("ORD_NEGATIVE"))
				.andExpect(jsonPath("$.findings[0].severity").value("ERROR"))
				.andExpect(jsonPath("$.findings[0].entityType").value("ORDER"))
				.andExpect(jsonPath("$.findings[0].entityId").value(7))
				.andExpect(jsonPath("$.findings[0].entityCode").value("it-order"))
				.andExpect(jsonPath("$.findings[0].detail").value("negative amount"));
		verify(reconciliationService).reconcile();
	}

	private RoleName role(String name) {
		return switch (name) {
			case "user" -> RoleName.ROLE_USER;
			case "manager" -> RoleName.ROLE_MANAGER;
			case "shipper" -> RoleName.ROLE_SHIPPER;
			default -> throw new IllegalArgumentException(name);
		};
	}

	private Cookie cookie(String email, RoleName role) {
		return new Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(details(email, role)));
	}

	private CustomUserDetails details(String email, RoleName roleName) {
		User user = new User();
		user.setId(1L);
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
