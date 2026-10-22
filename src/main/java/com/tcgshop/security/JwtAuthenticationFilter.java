package com.tcgshop.security;

import com.tcgshop.service.InvalidRefreshTokenException;
import com.tcgshop.service.RefreshTokenReuseException;
import com.tcgshop.service.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Xác thực mỗi request bằng access JWT, đồng thời lấy trạng thái tài khoản mới nhất từ DB. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private final JwtProvider jwtProvider;
	private final CookieUtil cookieUtil;
	private final UserDetailsServiceImpl userDetailsService;
	private final RefreshTokenService refreshTokenService;

	public JwtAuthenticationFilter(JwtProvider jwtProvider, CookieUtil cookieUtil,
			UserDetailsServiceImpl userDetailsService, RefreshTokenService refreshTokenService) {
		this.jwtProvider = jwtProvider;
		this.cookieUtil = cookieUtil;
		this.userDetailsService = userDetailsService;
		this.refreshTokenService = refreshTokenService;
	}

	/** Bỏ qua tài nguyên tĩnh để không kiểm tra JWT và không truy vấn DB cho CSS/JS/ảnh. */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.equals("/favicon.ico") || path.startsWith("/css/") || path.startsWith("/js/")
				|| path.startsWith("/img/") || path.startsWith("/lib/");
	}

	/**
	 * Ưu tiên access token trong cookie theo cấu hình; chỉ khi không có cookie mới đọc Bearer.
	 * Chữ ký và hạn dùng được kiểm tra trước khi dùng subject để nạp user. Claim role trong token
	 * không được tin cậy để phân quyền: authorities luôn lấy từ bản ghi DB hiện tại.
	 * Việc nạp lại user tạo thêm một truy vấn DB cho mỗi request có JWT hợp lệ. Đây là đánh đổi
	 * có chủ đích của đồ án để khi tài khoản bị khóa/vô hiệu hóa hoặc đổi role thì quyền cập nhật
	 * ngay, thay vì tiếp tục dùng trạng thái cũ nằm trong access token.
	 * JWT sai, hết hạn, user không tồn tại, chưa kích hoạt hoặc đang bị khóa đều được coi như
	 * request chưa đăng nhập; filter không ném lỗi để authorization phía sau quyết định phản hồi.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			Optional<String> token = cookieUtil.readAccessToken(request);
			if (token.isEmpty()) {
				String authorization = request.getHeader(AUTHORIZATION);
				if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
					token = Optional.of(authorization.substring(BEARER_PREFIX.length()).trim());
				}
			}
			if (token.isPresent() && !token.get().isBlank()) {
				String accessToken = token.get();
				if (jwtProvider.validateToken(accessToken)) {
					authenticate(accessToken, request);
				} else if (jwtProvider.isExpiredToken(accessToken) && !isExplicitRefreshRequest(request)) {
					refresh(request, response);
				}
			} else if (!isExplicitRefreshRequest(request)) {
				refresh(request, response);
			}
		}
		filterChain.doFilter(request, response);
	}

	private boolean isExplicitRefreshRequest(HttpServletRequest request) {
		String path = request.getServletPath();
		// Không xoay refresh token trước logout; nếu xoay, controller chỉ thu hồi token cũ đã bị thay thế.
		return "/auth/refresh".equals(path) || "/auth/logout".equals(path);
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			UserDetails user = userDetailsService.loadUserByUsername(jwtProvider.getEmail(token));
			if (user.isEnabled() && user.isAccountNonLocked()) {
				UsernamePasswordAuthenticationToken authentication =
						UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (UsernameNotFoundException | IllegalArgumentException | JwtException exception) {
			log.debug("JWT ignored because its account could not be loaded");
		}
	}

	private void refresh(HttpServletRequest request, HttpServletResponse response) {
		Optional<String> rawRefreshToken = cookieUtil.readRefreshToken(request);
		if (rawRefreshToken.isEmpty()) {
			cookieUtil.clearAuthCookies(response);
			return;
		}
		try {
			RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken.get());
			UserDetails user = userDetailsService.loadUserByUsername(rotation.email());
			if (!(user instanceof CustomUserDetails customUser)
					|| !user.isEnabled() || !user.isAccountNonLocked()) {
				refreshTokenService.revokeAllByUser(rotation.userId());
				cookieUtil.clearAuthCookies(response);
				return;
			}
			String newAccessToken = jwtProvider.generateAccessToken(customUser);
			cookieUtil.addAccessTokenCookie(response, newAccessToken);
			cookieUtil.addRefreshTokenCookie(response, rotation.rawToken());
			setAuthentication(customUser, request);
		} catch (InvalidRefreshTokenException | RefreshTokenReuseException
				| UsernameNotFoundException | IllegalArgumentException | JwtException exception) {
			log.debug("Refresh token rejected; clearing authentication cookies");
			cookieUtil.clearAuthCookies(response);
		}
	}

	private void setAuthentication(UserDetails user, HttpServletRequest request) {
		UsernamePasswordAuthenticationToken authentication =
				UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
