package com.tcgshop.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.response.AddToCartResponse;
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
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.CartService;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
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

@WebMvcTest(controllers = {com.tcgshop.controller.rest.CartRestController.class,
		com.tcgshop.controller.web.CartController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class CartControllerMvcTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private CartService cartService;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUpUsers() {
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0);
			return switch (email) {
				case "user@example.com" -> details(email, RoleName.ROLE_USER);
				case "vendor@example.com" -> details(email, RoleName.ROLE_USER, RoleName.ROLE_VENDOR);
				case "shipper@example.com" -> details(email, RoleName.ROLE_SHIPPER);
			case "manager@example.com" -> details(email, RoleName.ROLE_MANAGER);
				default -> throw new org.springframework.security.core.userdetails.UsernameNotFoundException(email);
			};
		});
	}

	@Test
	void anonymousCartApiReturns401AndWebCartRedirectsWithEncodedReturnUrl() throws Exception {
		mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
		mockMvc.perform(post("/api/cart/items").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":1,\"quantity\":2}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
		mockMvc.perform(get("/cart")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/auth/login?returnUrl=%2Fcart"));
	}

	@Test
	void shipperAndManagerAreDeniedButUserAndVendorCanAccessCart() throws Exception {
		for (String email : List.of("shipper@example.com", "manager@example.com")) {
			mockMvc.perform(get("/api/cart").cookie(accessCookie(token(email))))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
		}
		when(cartService.getCart()).thenReturn(emptyCart());
		mockMvc.perform(get("/api/cart").cookie(accessCookie(token("user@example.com"))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/cart").cookie(accessCookie(token("vendor@example.com"))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/cart").cookie(accessCookie(token("user@example.com"))))
				.andExpect(status().isOk()).andExpect(view().name("user/cart"));
	}

	@Test
	void addItemValidatesBodyAndReturnsOnlyContractKeys() throws Exception {
		String userCookie = token("user@example.com");
		for (String body : List.of("{\"productId\":1,\"quantity\":0}",
				"{\"productId\":1,\"quantity\":100}", "{\"quantity\":2}")) {
			mockMvc.perform(post("/api/cart/items").cookie(accessCookie(userCookie)).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
		}
		when(cartService.addItem(any(AddToCartRequest.class)))
				.thenReturn(new AddToCartResponse(true, "Đã thêm vào giỏ hàng", 3));
		mockMvc.perform(post("/api/cart/items").cookie(accessCookie(userCookie)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(3)))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Đã thêm vào giỏ hàng"))
				.andExpect(jsonPath("$.cartCount").value(3));
	}

	@Test
	void addItemRequiresCsrfAndMissingProductIs404Json() throws Exception {
		String userCookie = token("user@example.com");
		mockMvc.perform(post("/api/cart/items").cookie(accessCookie(userCookie))
				.contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":2}"))
				.andExpect(status().isForbidden());
		when(cartService.addItem(any(AddToCartRequest.class)))
				.thenThrow(new ResourceNotFoundException("Sản phẩm", 999L));
		mockMvc.perform(post("/api/cart/items").cookie(accessCookie(userCookie)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"productId\":999,\"quantity\":2}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path").value("/api/cart/items"));
	}

	private String token(String email) {
		return jwtProvider.generateAccessToken(details(email,
				email.startsWith("vendor") ? new RoleName[] {RoleName.ROLE_USER, RoleName.ROLE_VENDOR}
						: new RoleName[] {roleFor(email)}));
	}

	private RoleName roleFor(String email) {
		if (email.startsWith("shipper")) return RoleName.ROLE_SHIPPER;
		if (email.startsWith("manager")) return RoleName.ROLE_MANAGER;
		return RoleName.ROLE_USER;
	}

	private Cookie accessCookie(String token) { return new Cookie("ACCESS_TOKEN", token); }

	private CustomUserDetails details(String email, RoleName... roles) {
		User user = new User();
		user.setId(42L);
		user.setEmail(email);
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		Set<Role> userRoles = EnumSet.copyOf(List.of(roles)).stream().map(roleName -> {
			Role role = new Role();
			role.setName(roleName);
			return role;
		}).collect(java.util.stream.Collectors.toSet());
		user.setRoles(userRoles);
		return new CustomUserDetails(user);
	}

	private com.tcgshop.dto.response.CartResponse emptyCart() {
		return new com.tcgshop.dto.response.CartResponse(List.of(), 0, 0, java.math.BigDecimal.ZERO, false);
	}
}
