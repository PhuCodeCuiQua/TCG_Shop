package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.OrderDetailItemResponse;
import com.tcgshop.dto.response.OrderListItemResponse;
import com.tcgshop.dto.response.OrderTabResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.TimelineEntryResponse;
import com.tcgshop.dto.response.UserOrderListResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.UserOrderService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserOrderController.class, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class UserOrderControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private UserOrderService userOrderService;

	private final Map<String, CustomUserDetails> principals = new java.util.HashMap<>();

	@BeforeEach
	void setUpPrincipals() {
		principals.clear();
		principals.put("user@example.com", details("user@example.com", false, RoleName.ROLE_USER));
		principals.put("vendor@example.com", details("vendor@example.com", false, RoleName.ROLE_USER, RoleName.ROLE_VENDOR));
		principals.put("shipper@example.com", details("shipper@example.com", false, RoleName.ROLE_SHIPPER));
		principals.put("manager@example.com", details("manager@example.com", false, RoleName.ROLE_MANAGER));
		principals.put("forced@example.com", details("forced@example.com", true, RoleName.ROLE_USER));
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			CustomUserDetails found = principals.get(invocation.getArgument(0));
			if (found == null) throw new UsernameNotFoundException("not found");
			return found;
		});
	}

	@Test
	void anonymousRedirectsAndPreservesQueryInReturnUrl() throws Exception {
		mockMvc.perform(get("/orders?tab=new&q=AB+12"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login?returnUrl=%2Forders%3Ftab%3Dnew%26q%3DAB%2B12"));
	}

	@Test
	void userAndVendorCanViewListWithModelAndNoStoreHeader() throws Exception {
		when(userOrderService.listMyOrders("all", null, 0)).thenReturn(listResponse());
		for (String email : List.of("user@example.com", "vendor@example.com")) {
			mockMvc.perform(get("/orders").cookie(cookie(token(email))))
					.andExpect(status().isOk()).andExpect(view().name("user/orders/index"))
					.andExpect(model().attributeExists("orderList"))
					.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
		}
	}

	@Test
	void staffRolesAreDeniedAndMustChangePasswordIsRedirected() throws Exception {
		for (String email : List.of("shipper@example.com", "manager@example.com")) {
			mockMvc.perform(get("/orders").cookie(cookie(token(email))))
					.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/orders").cookie(cookie(token("forced@example.com"))))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile/change-password"));
	}

	@Test
	void passesUnusualTabNegativePageAndLongKeywordToServiceWithoutFailingPage() throws Exception {
		String longKeyword = "x".repeat(75);
		when(userOrderService.listMyOrders("surprise", longKeyword, -4)).thenReturn(listResponse());
		mockMvc.perform(get("/orders").cookie(cookie(token("user@example.com")))
				.param("tab", "surprise").param("q", longKeyword).param("page", "-4"))
				.andExpect(status().isOk());
		verify(userOrderService).listMyOrders("surprise", longKeyword, -4);
	}

	@Test
	void detailRendersEscapedNoteAndUsesDetailModel() throws Exception {
		when(userOrderService.getMyOrder(8L)).thenReturn(detailWithScriptNote());
		mockMvc.perform(get("/orders/8").cookie(cookie(token("user@example.com"))))
				.andExpect(status().isOk()).andExpect(view().name("user/orders/detail"))
				.andExpect(model().attributeExists("order"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ORDER-IT-8")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/products/77#reviews")))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
	}

	@Test
	void malformedIdIsBadRequestAndMissingOwnedOrderUsesNotFoundView() throws Exception {
		mockMvc.perform(get("/orders/abc").cookie(cookie(token("user@example.com"))))
				.andExpect(status().isBadRequest());
		when(userOrderService.getMyOrder(999L)).thenThrow(new ResourceNotFoundException("Đơn hàng", 999L));
		mockMvc.perform(get("/orders/999").cookie(cookie(token("user@example.com"))))
				.andExpect(status().isNotFound()).andExpect(view().name("error/404"));
	}

	private String token(String email) { return jwtProvider.generateAccessToken(principals.get(email)); }
	private Cookie cookie(String value) { return new Cookie("ACCESS_TOKEN", value); }

	private CustomUserDetails details(String email, boolean mustChangePassword, RoleName... roleNames) {
		User user = new User();
		user.setId(42L);
		user.setEmail(email);
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		user.setMustChangePassword(mustChangePassword);
		Set<Role> roles = EnumSet.copyOf(List.of(roleNames)).stream().map(name -> {
			Role role = new Role(); role.setName(name); return role;
		}).collect(java.util.stream.Collectors.toSet());
		user.setRoles(roles);
		return new CustomUserDetails(user);
	}

	private UserOrderListResponse listResponse() {
		OrderListItemResponse item = new OrderListItemResponse(8L, "ORDER-IT-8", "GROUP-IT-1", 3L,
				"Cửa hàng thử nghiệm", OrderStatus.NEW, OrderStatus.NEW.getDisplayName(), "warning",
				LocalDateTime.of(2026, 1, 2, 3, 4), new BigDecimal("125000"), 0, List.of(), 0);
		return new UserOrderListResponse(List.of(new OrderTabResponse("all", "Tất cả", 1, true)),
				new PageResponse<>(List.of(item), 0, 10, 1, 1, true, true), "all", null);
	}

	private OrderDetailResponse detailWithScriptNote() {
		return new OrderDetailResponse(8L, "ORDER-IT-8", "GROUP-IT-1", OrderStatus.DELIVERED,
				OrderStatus.DELIVERED.getDisplayName(), "success", LocalDateTime.of(2026, 1, 2, 3, 4), null,
				3L, "Cửa hàng thử nghiệm", List.of(new OrderDetailItemResponse(77L, "Thẻ <hi>", null,
						"Near Mint", new BigDecimal("125000"), 1, new BigDecimal("125000"))), new BigDecimal("125000"), BigDecimal.ZERO,
				new BigDecimal("20000"), new BigDecimal("145000"), "GHN", "Khách thử", "0900000000",
				"Địa chỉ thử", "<script>alert(1)</script>", null, "COD", "Chưa thanh toán", null,
			null, null, List.of(), List.of(new TimelineEntryResponse("Đã giao", "success",
					LocalDateTime.of(2026, 1, 2, 4, 0), "Người giao hàng", null)), List.of(), false, false);
	}
}
