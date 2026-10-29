package com.tcgshop.security;

import com.tcgshop.config.CookieProperties;
import com.tcgshop.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

	private final JwtProperties jwtProperties;
	private final CookieProperties cookieProperties;

	public CookieUtil(JwtProperties jwtProperties, CookieProperties cookieProperties) {
		this.jwtProperties = jwtProperties;
		this.cookieProperties = cookieProperties;
	}

	public void addAccessTokenCookie(HttpServletResponse response, String token) {
		addCookie(response, jwtProperties.accessCookieName(), token,
				Duration.ofMinutes(jwtProperties.accessExpirationMinutes()));
	}

	public void addRefreshTokenCookie(HttpServletResponse response, String token) {
		addCookie(response, jwtProperties.refreshCookieName(), token,
				Duration.ofDays(jwtProperties.refreshExpirationDays()));
	}

	public void clearAuthCookies(HttpServletResponse response) {
		addCookie(response, jwtProperties.accessCookieName(), "", Duration.ZERO);
		addCookie(response, jwtProperties.refreshCookieName(), "", Duration.ZERO);
	}

	public Optional<String> readAccessToken(HttpServletRequest request) {
		return readCookie(request, jwtProperties.accessCookieName());
	}

	public Optional<String> readRefreshToken(HttpServletRequest request) {
		return readCookie(request, jwtProperties.refreshCookieName());
	}

	private void addCookie(
			HttpServletResponse response, String name, String value, Duration maxAge) {
		// Cookie xác thực luôn HttpOnly, SameSite=Lax và chỉ bật Secure theo cấu hình môi trường.
		ResponseCookie cookie = ResponseCookie.from(name, value)
				.httpOnly(true)
				.sameSite("Lax")
				.secure(cookieProperties.secure())
				.path("/")
				.maxAge(maxAge)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private Optional<String> readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue)
				.filter(value -> value != null && !value.isBlank())
				.findFirst();
	}
}
