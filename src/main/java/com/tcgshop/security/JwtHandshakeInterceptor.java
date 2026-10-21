package com.tcgshop.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Chạy ở bước handshake HTTP (/ws/**).
 * Đọc JWT từ HttpOnly cookie; nếu hợp lệ thì lưu Authentication vào session attributes.
 * LUÔN cho phép handshake đi tiếp: khách chưa đăng nhập vẫn được kết nối để nghe topic công khai.
 * Việc chặn kênh riêng nằm ở WebSocketAuthChannelInterceptor.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Khóa lưu Authentication trong session attributes của WebSocket. */
    public static final String AUTH_ATTRIBUTE = "WS_AUTHENTICATION";

    private final WebSocketTokenAuthenticator authenticator;
    private final String accessCookieName;

    public JwtHandshakeInterceptor(
            WebSocketTokenAuthenticator authenticator,
            // GIẢ ĐỊNH: tên cookie access token; chỉnh cho khớp với cookie Phú đặt khi đăng nhập
            @Value("${app.jwt.access-cookie-name:access_token}") String accessCookieName) {
        this.authenticator = authenticator;
        this.accessCookieName = accessCookieName;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = extractTokenFromCookie(httpRequest);
            authenticator.authenticate(token)
                    .ifPresent(auth -> attributes.put(AUTH_ATTRIBUTE, auth));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý sau handshake
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (accessCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}