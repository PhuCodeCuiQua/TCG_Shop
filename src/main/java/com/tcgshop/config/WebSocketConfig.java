package com.tcgshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.tcgshop.security.JwtHandshakeInterceptor;
import com.tcgshop.security.WebSocketAuthChannelInterceptor;

/**
 * Cấu hình WebSocket + STOMP.
 *  - Endpoint kết nối : /ws (có SockJS fallback)
 *  - Client gửi lên    : /app/**
 *  - Broker nội bộ     : /topic (công khai), /queue (riêng tư, đi kèm /user)
 *  - Prefix user       : /user  (vd client subscribe /user/queue/notifications)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            JwtHandshakeInterceptor jwtHandshakeInterceptor,
            WebSocketAuthChannelInterceptor authChannelInterceptor,
            // Chống Cross-Site WebSocket Hijacking: chỉ cho origin của web mình.
            // Khi deploy/ngrok nhớ khai báo thêm origin trong application-local.properties
            @Value("${app.websocket.allowed-origin-patterns:http://localhost:*}") String[] allowedOriginPatterns) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.authChannelInterceptor = authChannelInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                // Đọc JWT từ cookie ở bước handshake (khách vẫn được đi tiếp)
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Gán Principal khi CONNECT và chặn SUBSCRIBE/SEND không hợp lệ
        registration.interceptors(authChannelInterceptor);
    }
}