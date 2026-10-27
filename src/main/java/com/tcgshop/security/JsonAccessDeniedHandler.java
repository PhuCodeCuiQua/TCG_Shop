package com.tcgshop.security;

import com.tcgshop.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	public JsonAccessDeniedHandler(JsonAuthenticationEntryPoint authenticationEntryPoint) {
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException exception) throws IOException, ServletException {
		if (RequestUtils.isApiRequest(request)) {
			if (exception instanceof CsrfException) {
				authenticationEntryPoint.writeJson(request, response, HttpServletResponse.SC_FORBIDDEN,
						"Invalid CSRF token",
						"CSRF token thiếu hoặc không hợp lệ. Vui lòng tải lại trang rồi thử lại.");
				return;
			}
			authenticationEntryPoint.writeJson(request, response, HttpServletResponse.SC_FORBIDDEN,
					"Forbidden", "You do not have permission to access this resource");
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
	}
}
