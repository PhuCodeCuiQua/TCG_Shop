package com.tcgshop.security;

import com.tcgshop.config.CookieProperties;
import com.tcgshop.config.JwtProperties;
import com.tcgshop.entity.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})
public class JwtProvider {

	private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
	private static final long CLOCK_SKEW_SECONDS = 30;
	private static final String USER_ID_CLAIM = "uid";
	private static final String ROLES_CLAIM = "roles";

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtProvider(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = createSigningKey(properties.secret());
	}

	// Tạo access token đã ký, chỉ đưa email, user id và authorities cần thiết vào claims.
	public String generateAccessToken(CustomUserDetails userDetails) {
		Instant now = Instant.now();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.toList();

		return Jwts.builder()
				.subject(userDetails.getEmail())
				.claim(USER_ID_CLAIM, userDetails.getId())
				.claim(ROLES_CLAIM, roles)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(Duration.ofMinutes(properties.accessExpirationMinutes()))))
				.signWith(signingKey)
				.compact();
	}

	// Kiểm tra chữ ký và hạn dùng với khoảng lệch đồng hồ cho phép 30 giây; không ghi token vào log.
	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (ExpiredJwtException exception) {
			log.debug("JWT rejected: token expired");
		} catch (SignatureException exception) {
			log.warn("JWT rejected: invalid signature");
		} catch (MalformedJwtException exception) {
			log.warn("JWT rejected: malformed token");
		} catch (UnsupportedJwtException exception) {
			log.debug("JWT rejected: unsupported token");
		} catch (IllegalArgumentException exception) {
			log.debug("JWT rejected: token is missing or empty");
		} catch (JwtException exception) {
			log.debug("JWT rejected: token validation failed");
		}
		return false;
	}

	/** Distinguishes an expired signed JWT from a malformed or incorrectly signed token. */
	public boolean isExpiredToken(String token) {
		try {
			parseToken(token);
			return false;
		} catch (ExpiredJwtException exception) {
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public String getEmail(String token) {
		return parseToken(token).getSubject();
	}

	public Long getUserId(String token) {
		Object userId = parseToken(token).get(USER_ID_CLAIM);
		if (userId instanceof Number number) {
			return number.longValue();
		}
		throw new IllegalArgumentException("JWT user id claim is missing or invalid");
	}

	public Set<RoleName> getRoles(String token) {
		Object roles = parseToken(token).get(ROLES_CLAIM);
		if (!(roles instanceof List<?> roleNames)) {
			throw new IllegalArgumentException("JWT roles claim is missing or invalid");
		}
		return roleNames.stream()
				.map(roleName -> {
					if (!(roleName instanceof String name)) {
						throw new IllegalArgumentException("JWT roles claim contains an invalid value");
					}
					return RoleName.valueOf(name);
				})
				.collect(Collectors.toUnmodifiableSet());
	}

	private Claims parseToken(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.clockSkewSeconds(CLOCK_SKEW_SECONDS)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private static SecretKey createSigningKey(String secret) {
		if (secret == null || secret.isBlank()
				|| secret.toUpperCase().contains("CHANGE_ME")) {
			throw new IllegalStateException(
					"app.jwt.secret is required and must not contain the CHANGE_ME placeholder");
		}

		final byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(secret);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("app.jwt.secret must be valid Base64", exception);
		}

		String decodedSecret = new String(keyBytes, StandardCharsets.UTF_8);
		if (decodedSecret.contains("CHANGE_ME")) {
			throw new IllegalStateException(
					"app.jwt.secret must not contain the CHANGE_ME placeholder");
		}
		if (keyBytes.length < 32) {
			throw new IllegalStateException(
					"app.jwt.secret must decode to at least 32 bytes for HMAC signing");
		}

		return Keys.hmacShaKeyFor(keyBytes);
	}
}
