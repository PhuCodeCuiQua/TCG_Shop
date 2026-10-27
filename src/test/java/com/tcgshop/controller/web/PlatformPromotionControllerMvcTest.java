package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.PlatformPromotionService;
import com.tcgshop.service.PlatformCouponAdminService;
import com.tcgshop.service.RefreshTokenService;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PlatformPromotionController.class, PlatformCouponAdminController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		CurrentUser.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class PlatformPromotionControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private PlatformPromotionService promotionService;
	@MockBean private PlatformCouponAdminService couponService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		when(promotionService.search(isNull(), isNull(), any())).thenReturn(PageResponse.from(Page.empty()));
		when(couponService.search(isNull(), any())).thenReturn(PageResponse.from(Page.empty()));
	}

	@Test
	void adminJwtCanOpenBothAdminCreatePages() throws Exception {
		CustomUserDetails admin = details(RoleName.ROLE_ADMIN);
		when(userDetailsService.loadUserByUsername("admin@tcgshop.local")).thenReturn(admin);
		var cookie = new jakarta.servlet.http.Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(admin));
		mockMvc.perform(get("/admin/promotions/new").cookie(cookie))
				.andExpect(status().isOk()).andExpect(view().name("manager/promotions/form"));
		mockMvc.perform(get("/admin/platform-coupons/new").cookie(cookie))
				.andExpect(status().isOk()).andExpect(view().name("manager/platform-coupons/form"));
	}

	@Test
	void adminCanViewBothRoutesWithAdminActions() throws Exception {
		for (String path : new String[] {"/admin/promotions", "/manager/promotions"}) {
			mockMvc.perform(get(path).with(user(details(RoleName.ROLE_ADMIN))))
					.andExpect(status().isOk())
					.andExpect(view().name("manager/promotions/list"))
					.andExpect(model().attribute("dashboardRole", "ADMIN"))
					.andExpect(model().attribute("basePath", "/admin/promotions"))
					.andExpect(model().attribute("canManage", true));
		}
	}

	@Test
	void onlyAdminCanOpenCreateFormThroughBothAliases() throws Exception {
		for (RoleName role : new RoleName[] {RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER}) {
			String basePath = role == RoleName.ROLE_ADMIN ? "/admin/promotions" : "/manager/promotions";
			for (String path : new String[] {"/admin/promotions", "/manager/promotions"}) {
				if (role == RoleName.ROLE_MANAGER && path.startsWith("/admin/")) {
					mockMvc.perform(get(path).with(user(details(role)))).andExpect(status().isForbidden());
					continue;
				}
				mockMvc.perform(get(path).with(user(details(role))))
						.andExpect(status().isOk())
						.andExpect(model().attribute("canManage", true))
						.andExpect(content().string(role == RoleName.ROLE_ADMIN
								? containsString(basePath + "/new") : org.hamcrest.Matchers.not(containsString("Tạo khuyến mãi"))));
				if (role == RoleName.ROLE_MANAGER) {
					mockMvc.perform(get(path + "/new").with(user(details(role))))
							.andExpect(status().isForbidden());
					mockMvc.perform(post(path + "/new").with(user(details(role))).with(csrf()))
							.andExpect(status().isForbidden());
					mockMvc.perform(post(path).with(user(details(role))).with(csrf()))
							.andExpect(status().isForbidden());
					continue;
				}
				mockMvc.perform(get(path + "/new").with(user(details(role))))
						.andExpect(status().isOk())
						.andExpect(view().name("manager/promotions/form"))
						.andExpect(model().attribute("formAction", basePath + "/new"));
				mockMvc.perform(post(path + "/new").with(user(details(role))).with(csrf()))
						.andExpect(status().isOk())
						.andExpect(view().name("manager/promotions/form"));
			}
		}
	}

	@Test
	void bothRolesCanPostPromotionsAndCsrfRemainsRequired() throws Exception {
		for (RoleName role : new RoleName[] {RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER}) {
			String basePath = role == RoleName.ROLE_ADMIN ? "/admin/promotions" : "/manager/promotions";
			for (String path : new String[] {"/admin/promotions", "/manager/promotions"}) {
				if (role == RoleName.ROLE_MANAGER && path.startsWith("/admin/")) {
					mockMvc.perform(post(path).with(user(details(role))).with(csrf()).param("promotionId", "5"))
							.andExpect(status().isForbidden());
					continue;
				}
				mockMvc.perform(post(path).with(user(details(role))).with(csrf())
						.param("promotionId", "5")
						.param("title", "Platform shipping promotion").param("type", "SHIPPING_DISCOUNT")
						.param("discountAmount", "10000").param("minOrderValue", "100000")
						.param("startDate", "2026-10-01T00:00:00").param("endDate", "2026-12-31T23:59:00")
						.param("status", "ACTIVE"))
						.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(basePath));
				mockMvc.perform(post(path).with(user(details(role))))
						.andExpect(status().isForbidden());
			}
		}
	}

	@Test
	void couponCreationIsAdminOnlyWhileBothRolesMayManageExistingCoupons() throws Exception {
		for (RoleName role : new RoleName[] {RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER}) {
			for (String path : new String[] {"/admin/platform-coupons", "/manager/platform-coupons"}) {
				if (role == RoleName.ROLE_MANAGER && path.startsWith("/admin/")) {
					mockMvc.perform(get(path).with(user(details(role)))).andExpect(status().isForbidden());
					continue;
				}
				mockMvc.perform(get(path).with(user(details(role))))
						.andExpect(status().isOk())
						.andExpect(model().attribute("canManage", true))
						.andExpect(content().string(role == RoleName.ROLE_ADMIN
								? containsString("Tạo mã giảm giá") : org.hamcrest.Matchers.not(containsString("Tạo mã giảm giá"))));
				mockMvc.perform(get(path + "/new").with(user(details(role))))
						.andExpect(role == RoleName.ROLE_ADMIN ? status().isOk() : status().isForbidden());
				if (role == RoleName.ROLE_ADMIN) {
					mockMvc.perform(post(path + "/new").with(user(details(role))).with(csrf()))
							.andExpect(status().isOk())
							.andExpect(view().name("manager/platform-coupons/form"));
				}
				if (role == RoleName.ROLE_MANAGER) {
					mockMvc.perform(post(path + "/new").with(user(details(role))).with(csrf()))
							.andExpect(status().isForbidden());
					mockMvc.perform(post(path).with(user(details(role))).with(csrf()))
							.andExpect(status().isForbidden());
				}
				mockMvc.perform(post(path + "/5/status").with(user(details(role))).with(csrf())
						.param("status", "INACTIVE")).andExpect(status().is3xxRedirection());
			}
		}
	}

	@Test
	void managerStillCannotEnterUnrelatedAdminModules() throws Exception {
		mockMvc.perform(get("/admin/commissions").with(user(details(RoleName.ROLE_MANAGER))))
				.andExpect(status().isForbidden());
	}

	@Test
	void ordinaryUserCannotViewPlatformPromotions() throws Exception {
		for (String path : new String[] {"/admin/promotions", "/manager/promotions"}) {
			mockMvc.perform(get(path).with(user(details(RoleName.ROLE_USER))))
					.andExpect(status().isForbidden());
		}
	}

	private CustomUserDetails details(RoleName roleName) {
		User account = new User();
		account.setId(1L);
		account.setEmail(roleName == RoleName.ROLE_ADMIN ? "admin@tcgshop.local" : "other@example.com");
		account.setFullName("Promotion test account");
		account.setPassword("encoded-test-password");
		account.setStatus(UserStatus.ACTIVE);
		Role role = new Role();
		role.setName(roleName);
		account.setRoles(new HashSet<>(Set.of(role)));
		return new CustomUserDetails(account);
	}
}
