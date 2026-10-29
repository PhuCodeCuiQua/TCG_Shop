package com.tcgshop.controller.rest;

import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.InvalidRefreshTokenException;
import com.tcgshop.service.RefreshTokenReuseException;
import com.tcgshop.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint REST mỏng để client chủ động xoay refresh token. */
@RestController
public class AuthRefreshRestController {
	private final RefreshTokenService refreshTokenService;
	private final UserDetailsServiceImpl userDetailsService;
	private final JwtProvider jwtProvider;
	private final CookieUtil cookieUtil;

	public AuthRefreshRestController(RefreshTokenService refreshTokenService,
			UserDetailsServiceImpl userDetailsService, JwtProvider jwtProvider, CookieUtil cookieUtil) {
		this.refreshTokenService = refreshTokenService;
		this.userDetailsService = userDetailsService;
		this.jwtProvider = jwtProvider;
		this.cookieUtil = cookieUtil;
	}

	@PostMapping("/auth/refresh")
	public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
		Optional<String> rawToken = cookieUtil.readRefreshToken(request);
		if (rawToken.isEmpty()) {
			cookieUtil.clearAuthCookies(response);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawToken.get());
			UserDetails user = userDetailsService.loadUserByUsername(rotation.email());
			if (!(user instanceof CustomUserDetails customUser)
					|| !user.isEnabled() || !user.isAccountNonLocked()) {
				refreshTokenService.revokeAllByUser(rotation.userId());
				cookieUtil.clearAuthCookies(response);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			cookieUtil.addAccessTokenCookie(response, jwtProvider.generateAccessToken(customUser));
			cookieUtil.addRefreshTokenCookie(response, rotation.rawToken());
			return ResponseEntity.noContent().build();
		} catch (InvalidRefreshTokenException | RefreshTokenReuseException | UsernameNotFoundException exception) {
			cookieUtil.clearAuthCookies(response);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}
}
