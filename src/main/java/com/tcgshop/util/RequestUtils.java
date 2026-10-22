package com.tcgshop.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public final class RequestUtils {
	private RequestUtils() { }

	public static boolean isApiRequest(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String path = contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)
				? requestUri.substring(contextPath.length()) : requestUri;
		if (path.startsWith("/api/")) return true;

		if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) return true;

		String accept = request.getHeader("Accept");
		if (accept == null) return false;
		String normalizedAccept = accept.toLowerCase(Locale.ROOT);
		return normalizedAccept.contains("application/json") && !normalizedAccept.contains("text/html");
	}
}
