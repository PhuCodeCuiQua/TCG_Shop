package com.tcgshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcgshop.dto.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Buộc tài khoản được cấp mật khẩu tạm thời đổi mật khẩu trước khi dùng chức năng khác. */
public class MustChangePasswordFilter extends OncePerRequestFilter {
	private static final String MESSAGE = "Bạn cần đổi mật khẩu trước khi tiếp tục";
	private final ObjectMapper objectMapper;

	public MustChangePasswordFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.getPrincipal() instanceof CustomUserDetails user
				&& user.isMustChangePassword() && !isAllowed(request.getServletPath())) {
			if (request.getServletPath().startsWith("/api/")) {
				writeApiError(request, response);
			} else {
				response.sendRedirect(request.getContextPath() + "/profile/change-password");
			}
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isAllowed(String path) {
		return path.equals("/profile/change-password") || path.equals("/auth/logout")
				|| path.equals("/error") || path.equals("/favicon.ico")
				|| path.startsWith("/css/") || path.startsWith("/js/")
				|| path.startsWith("/img/") || path.startsWith("/lib/");
	}

	private void writeApiError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
				? uri.substring(contextPath.length()) : uri;
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(), MESSAGE, path, null, null);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
