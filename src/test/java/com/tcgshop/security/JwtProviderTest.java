package com.tcgshop.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.config.JwtProperties;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

	private static final byte[] SECRET_BYTES =
			"0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);

	private JwtProvider jwtProvider;
	private CustomUserDetails userDetails;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider(new JwtProperties(SECRET, 30, 7, "ACCESS_TOKEN", "REFRESH_TOKEN"));
		userDetails = createUserDetails();
	}

	@Test
	void generatesAndReadsValidAccessToken() {
		String token = jwtProvider.generateAccessToken(userDetails);

		assertTrue(jwtProvider.validateToken(token));
		assertEquals("buyer@example.com", jwtProvider.getEmail(token));
		assertEquals(42L, jwtProvider.getUserId(token));
		assertEquals(Set.of(RoleName.ROLE_USER), jwtProvider.getRoles(token));
	}

	@Test
	void rejectsExpiredToken() {
		String token = tokenSignedWith(
				Keys.hmacShaKeyFor(SECRET_BYTES),
				"buyer@example.com",
				42L,
				List.of("ROLE_USER"),
				Instant.now().minusSeconds(120),
				Instant.now().minusSeconds(60));

		assertFalse(jwtProvider.validateToken(token));
	}

	@Test
	void rejectsTokenSignedWithDifferentKey() {
		SecretKey otherKey = Keys.hmacShaKeyFor(
				"abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8));
		String token = tokenSignedWith(
				otherKey, "buyer@example.com", 42L, List.of("ROLE_USER"),
				Instant.now(), Instant.now().plusSeconds(60));

		assertFalse(jwtProvider.validateToken(token));
	}

	@Test
	void rejectsTokenWithModifiedContent() {
		String token = jwtProvider.generateAccessToken(userDetails);
		String[] segments = token.split("\\.");
		String payload = new String(
				Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
		String modifiedPayload = payload.replace("buyer@example.com", "fraud@example.com");
		segments[1] = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(modifiedPayload.getBytes(StandardCharsets.UTF_8));

		assertFalse(jwtProvider.validateToken(String.join(".", segments)));
	}

	@Test
	void rejectsGarbageToken() {
		assertFalse(jwtProvider.validateToken("not-a-jwt"));
	}

	@Test
	void rejectsSecretShorterThanThirtyTwoBytesAtConstruction() {
		String shortSecret = Base64.getEncoder()
				.encodeToString("too-short".getBytes(StandardCharsets.UTF_8));
		JwtProperties properties =
				new JwtProperties(shortSecret, 30, 7, "ACCESS_TOKEN", "REFRESH_TOKEN");

		IllegalStateException exception =
				assertThrows(IllegalStateException.class, () -> new JwtProvider(properties));

		assertTrue(exception.getMessage().contains("32 bytes"));
	}

	private CustomUserDetails createUserDetails() {
		User user = new User();
		user.setId(42L);
		user.setEmail("buyer@example.com");
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);

		Role role = new Role();
		role.setName(RoleName.ROLE_USER);
		user.setRoles(Set.of(role));
		return new CustomUserDetails(user);
	}

	private String tokenSignedWith(
			SecretKey key,
			String subject,
			long userId,
			List<String> roles,
			Instant issuedAt,
			Instant expiration) {
		return Jwts.builder()
				.subject(subject)
				.claim("uid", userId)
				.claim("roles", roles)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiration))
				.signWith(key)
				.compact();
	}
}
