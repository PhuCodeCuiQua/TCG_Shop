package com.tcgshop.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcgshop.security.CookieUtil;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.controller.rest.WishlistRestController;
import com.tcgshop.dto.response.WishlistToggleResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.service.WishlistService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthorizationTestController.class, WishlistRestController.class}, properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.jwt.access-expiration-minutes=30",
		"app.jwt.refresh-expiration-days=7",
		"app.jwt.access-cookie-name=ACCESS_TOKEN",
		"app.jwt.refresh-cookie-name=REFRESH_TOKEN",
		"app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class SecurityAuthorizationTest {
	private static final byte[] SECRET_BYTES = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private final Map<String, CustomUserDetails> users = new HashMap<>();

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtProvider jwtProvider;
	@MockBean private UserDetailsServiceImpl userDetailsService;
	@MockBean private RefreshTokenService refreshTokenService;
	@MockBean private WishlistService wishlistService;

	@BeforeEach
	void configureMockUsers() {
		users.clear();
		users.put("user@example.com", details("user@example.com", UserStatus.ACTIVE, RoleName.ROLE_USER));
		users.put("vendor@example.com", details("vendor@example.com", UserStatus.ACTIVE,
				RoleName.ROLE_USER, RoleName.ROLE_VENDOR));
		users.put("vendor-only@example.com", details("vendor-only@example.com", UserStatus.ACTIVE,
				RoleName.ROLE_VENDOR));
		users.put("shipper@example.com", details("shipper@example.com", UserStatus.ACTIVE, RoleName.ROLE_SHIPPER));
		users.put("manager@example.com", details("manager@example.com", UserStatus.ACTIVE, RoleName.ROLE_MANAGER));
		users.put("admin@example.com", details("admin@example.com", UserStatus.ACTIVE, RoleName.ROLE_ADMIN));
		users.put("blocked@example.com", details("blocked@example.com", UserStatus.BLOCKED, RoleName.ROLE_USER));
		users.put("inactive@example.com", details("inactive@example.com", UserStatus.INACTIVE, RoleName.ROLE_USER));
		when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation -> {
			String username = invocation.getArgument(0);
			CustomUserDetails details = users.get(username);
			if (details == null) throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);
			return details;
		});
	}

	@Test
	void errorDispatchPreservesAuthenticationWithoutRotatingTokensOrCreatingSession() throws Exception {
		var original = mockMvc.perform(get("/admin/ping")
				.cookie(accessCookie(token("vendor@example.com"))))
				.andExpect(status().isForbidden()).andReturn();
		var errorDispatch = get("/error").with(request -> {
			java.util.Collections.list(original.getRequest().getAttributeNames()).forEach(name ->
					request.setAttribute(name, original.getRequest().getAttribute(name)));
			request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
			return request;
		});
		var result = mockMvc.perform(errorDispatch).andExpect(status().isOk())
				.andExpect(content().string("vendor@example.com")).andReturn();
		org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false)).isNull();
		org.mockito.Mockito.verifyNoInteractions(refreshTokenService);
	}

	@Test
	void anonymousWebRedirectsApiGetsJson401AndPublicGetsWork() throws Exception {
		mockMvc.perform(get("/vendor/ping"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/auth/login?returnUrl=*"));
		mockMvc.perform(get("/api/vendor/ping").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/vendor/ping"));
		mockMvc.perform(get("/api/products/ping")).andExpect(status().isOk()).andExpect(content().string("ok"));
		mockMvc.perform(get("/api/public-test")).andExpect(status().isOk()).andExpect(content().string("ok"));
	}

	@Test
	void guestShoppingApisReturn401WithValidCsrfAndWebPagesRedirect() throws Exception {
		for (String path : List.of("/api/cart", "/api/wishlist/status?productIds=5", "/api/checkout/preview")) {
			mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}
		for (String path : List.of("/api/cart/items", "/api/wishlist/toggle/5", "/api/checkout/place")) {
			mockMvc.perform(post(path).accept(MediaType.APPLICATION_JSON)
					.cookie(new Cookie("XSRF-TOKEN", "guest-csrf-token"))
					.header("X-XSRF-TOKEN", "guest-csrf-token"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error").value("Unauthorized"));
		}
		for (String path : List.of("/cart", "/wishlist", "/checkout")) {
			mockMvc.perform(get(path)).andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern("/auth/login?returnUrl=*"));
		}
	}

	@Test
	void wrongRolesAreDeniedAndManagerAdminBoundaryIsCorrect() throws Exception {
		assertWebDenied("/vendor/ping", "user@example.com");
		assertApiDenied("/api/vendor/ping", "user@example.com");
		assertWebDenied("/admin/ping", "vendor@example.com");
		assertApiDenied("/api/admin/ping", "vendor@example.com");
		assertWebDenied("/admin/ping", "manager@example.com");
		assertApiDenied("/api/admin/ping", "manager@example.com");
		assertWebAllowed("/admin/ping", "admin@example.com");
		assertWebAllowed("/manager/ping", "admin@example.com");
		assertApiAllowed("/api/manager/ping", "admin@example.com");
	}

	@Test
	void vendorRetainsUserAccessAsWellAsVendorAccess() throws Exception {
		assertWebAllowed("/cart/ping", "vendor@example.com");
		assertWebAllowed("/vendor/ping", "vendor@example.com");
		assertWebAllowed("/shipper/ping", "shipper@example.com");
	}

	@Test
	void wishlistApiAllowsUserWithCsrfAndRejectsAccountsWithoutUserRole() throws Exception {
		when(wishlistService.toggle(5L)).thenReturn(new WishlistToggleResponse(true, 1));
		mockMvc.perform(post("/api/wishlist/toggle/5")
				.cookie(accessCookie(token("user@example.com")), new Cookie("XSRF-TOKEN", "test-csrf-token"))
				.header("X-XSRF-TOKEN", "test-csrf-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wishlisted").value(true))
				.andExpect(jsonPath("$.favoriteCount").value(1));

		mockMvc.perform(post("/api/wishlist/toggle/5")
				.cookie(accessCookie(token("user@example.com"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/wishlist/toggle/5")
				.cookie(accessCookie(token("vendor-only@example.com")), new Cookie("XSRF-TOKEN", "test-csrf-token"))
				.header("X-XSRF-TOKEN", "test-csrf-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
	}

	@Test
	void expiredTamperedAndWrongKeyTokensAreAnonymous() throws Exception {
		String valid = token("user@example.com");
		String[] parts = valid.split("\\.");
		parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
				"{\"sub\":\"user@example.com\",\"uid\":42,\"roles\":[\"ROLE_ADMIN\"]}"
					.getBytes(StandardCharsets.UTF_8));
		String tampered = String.join(".", parts);
		String wrongKey = signedToken("user@example.com", Keys.hmacShaKeyFor(
				"abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8)),
				Instant.now().minusSeconds(1), Instant.now().plusSeconds(120));
		String expired = signedToken("user@example.com", Keys.hmacShaKeyFor(SECRET_BYTES),
				Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));
		for (String token : List.of(expired, tampered, wrongKey)) {
			mockMvc.perform(get("/vendor/ping").cookie(accessCookie(token)))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern("/auth/login?returnUrl=*"));
		}
	}

	@Test
	void blockedAndInactiveUsersCannotUseOtherwiseValidTokens() throws Exception {
		for (String email : List.of("blocked@example.com", "inactive@example.com")) {
			mockMvc.perform(get("/profile/ping").cookie(accessCookie(token(email))))
					.andExpect(status().is3xxRedirection());
		}
	}

	@Test
	void csrfProtectsPostsExceptMomoIpn() throws Exception {
		mockMvc.perform(post("/vendor/ping").cookie(accessCookie(token("vendor@example.com"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/vendor/ping")
				.cookie(accessCookie(token("vendor@example.com")), new Cookie("XSRF-TOKEN", "test-csrf-token"))
				.header("X-XSRF-TOKEN", "test-csrf-token"))
				.andExpect(status().isOk()).andExpect(content().string("ok"));
		mockMvc.perform(post("/payment/momo/ipn"))
				.andExpect(status().isOk()).andExpect(content().string("ok"));
	}

	@Test
	void getIssuesReadableCookieAndRawTokenWorksForHeaderAndForm() throws Exception {
		var page = mockMvc.perform(get("/api/products/ping"))
				.andExpect(status().isOk()).andReturn();
		Cookie csrf = page.getResponse().getCookie("XSRF-TOKEN");
		org.assertj.core.api.Assertions.assertThat(csrf).isNotNull();
		org.assertj.core.api.Assertions.assertThat(csrf.isHttpOnly()).isFalse();
		org.assertj.core.api.Assertions.assertThat(page.getRequest().getSession(false)).isNull();
		var requestToken = (org.springframework.security.web.csrf.CsrfToken)
				page.getRequest().getAttribute("_csrf");
		org.assertj.core.api.Assertions.assertThat(requestToken.getToken()).isEqualTo(csrf.getValue());
		mockMvc.perform(post("/vendor/ping")
				.cookie(accessCookie(token("vendor@example.com")), csrf)
				.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/vendor/ping")
				.cookie(accessCookie(token("vendor@example.com")), csrf)
				.param("_csrf", csrf.getValue()))
				.andExpect(status().isOk());
	}

	@Test
	void wrongMissingOrCookieOnlyTokensRemainForbiddenIncludingLogout() throws Exception {
		for (String path : List.of("/vendor/ping", "/auth/logout")) {
			mockMvc.perform(post(path).accept(MediaType.APPLICATION_JSON)
					.cookie(accessCookie(token("vendor@example.com")), new Cookie("XSRF-TOKEN", "raw-token")))
					.andExpect(status().isForbidden());
			mockMvc.perform(post(path).accept(MediaType.APPLICATION_JSON)
					.cookie(accessCookie(token("vendor@example.com")), new Cookie("XSRF-TOKEN", "raw-token"))
					.header("X-XSRF-TOKEN", "wrong-token"))
					.andExpect(status().isForbidden());
		}
		mockMvc.perform(post("/vendor/ping").cookie(accessCookie(token("vendor@example.com")))
				.header("X-XSRF-TOKEN", "token-without-cookie"))
				.andExpect(status().isForbidden());
	}

	@Test
	void xhrWebRequestsReceiveJsonErrorsAndVendorMayAccessCart() throws Exception {
		mockMvc.perform(get("/vendor/ping").header("X-Requested-With", "XMLHttpRequest"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Unauthorized"));
		mockMvc.perform(get("/admin/ping").cookie(accessCookie(token("manager@example.com")))
				.header("X-Requested-With", "XMLHttpRequest"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	private void assertWebDenied(String path, String email) throws Exception {
		mockMvc.perform(get(path).cookie(accessCookie(token(email))))
				.andExpect(status().isForbidden());
	}

	private void assertApiDenied(String path, String email) throws Exception {
		mockMvc.perform(get(path).cookie(accessCookie(token(email))))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	private void assertWebAllowed(String path, String email) throws Exception {
		mockMvc.perform(get(path).cookie(accessCookie(token(email))))
				.andExpect(status().isOk()).andExpect(content().string("ok"));
	}

	private void assertApiAllowed(String path, String email) throws Exception {
		mockMvc.perform(get(path).cookie(accessCookie(token(email))))
				.andExpect(status().isOk()).andExpect(content().string("ok"));
	}

	private String token(String email) { return jwtProvider.generateAccessToken(users.get(email)); }
	private Cookie accessCookie(String token) { return new Cookie("ACCESS_TOKEN", token); }

	private CustomUserDetails details(String email, UserStatus status, RoleName... roleNames) {
		User user = new User();
		user.setId(42L);
		user.setEmail(email);
		user.setPassword("encoded-password");
		user.setStatus(status);
		Set<Role> roles = EnumSet.copyOf(List.of(roleNames)).stream().map(roleName -> {
			Role role = new Role();
			role.setName(roleName);
			return role;
		}).collect(java.util.stream.Collectors.toSet());
		user.setRoles(roles);
		return new CustomUserDetails(user);
	}

	private String signedToken(String subject, SecretKey key, Instant issuedAt, Instant expiration) {
		return Jwts.builder().subject(subject).claim("uid", 42L).claim("roles", List.of("ROLE_USER"))
				.issuedAt(Date.from(issuedAt)).expiration(Date.from(expiration)).signWith(key).compact();
	}

}
