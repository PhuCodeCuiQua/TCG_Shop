package com.tcgshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;
	public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		if (wantsJson(request)) {
			writeJson(request, response, HttpServletResponse.SC_UNAUTHORIZED,
					"Unauthorized", "Authentication is required");
			return;
		}
		String returnUrl = request.getRequestURI();
		if (request.getQueryString() != null) returnUrl += "?" + request.getQueryString();
		String encoded = URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
		response.sendRedirect(request.getContextPath() + "/auth/login?returnUrl=" + encoded);
	}

	static boolean wantsJson(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return path.startsWith("/api/")
				|| "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
				|| (request.getHeader("Accept") != null
						&& request.getHeader("Accept").contains(MediaType.APPLICATION_JSON_VALUE));
	}

	void writeJson(HttpServletRequest request, HttpServletResponse response,
			int status, String error, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", status);
		body.put("error", error);
		body.put("message", message);
		body.put("path", request.getRequestURI().substring(request.getContextPath().length()));
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
