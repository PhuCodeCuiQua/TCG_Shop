package com.tcgshop.security;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PUBLIC_TOPIC_PREFIX = "/topic/";
    private static final String USER_QUEUE_PREFIX = "/user/queue/";
    private static final String APP_PREFIX = "/app/";

    private final WebSocketTokenAuthenticator authenticator;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticateOnConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            checkSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            checkSend(accessor);
        }
        return message;
    }

    /** Gán Principal cho phiên. Không có token hợp lệ thì để user = null (khách). */
    private void authenticateOnConnect(StompHeaderAccessor accessor) {
        Authentication auth = null;

        // Cách 1 (trình duyệt): lấy Authentication đã lưu ở bước handshake
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null
                && sessionAttrs.get(JwtHandshakeInterceptor.AUTH_ATTRIBUTE) instanceof Authentication saved) {
            auth = saved;
        }

        // Cách 2 (REST/AJAX, client không dùng cookie): header Authorization: Bearer <token>
        if (auth == null) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                auth = authenticator.authenticate(header.substring(BEARER_PREFIX.length())).orElse(null);
            }
        }

        if (auth != null) {
            // Principal.getName() chính là tham số "user" khi gọi convertAndSendToUser(...)
            accessor.setUser(auth);
        }
    }

    private void checkSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        if (destination != null && destination.startsWith(PUBLIC_TOPIC_PREFIX)) {
            return; // topic công khai: ai cũng nghe được
        }
        if (destination != null && destination.startsWith(USER_QUEUE_PREFIX)) {
            if (user == null) {
                log.debug("WebSocket: khách bị từ chối subscribe {}", destination);
                throw new AccessDenied("Cần đăng nhập để đăng ký kênh riêng");
            }
            return;
        }
        // Chặn cả việc subscribe thẳng /queue/** (không qua /user) và mọi đường dẫn lạ
        throw new AccessDenied("Destination không được phép: " + destination);
    }

    private void checkSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (accessor.getUser() == null) {
            throw new AccessDenied("Cần đăng nhập để gửi tin");
        }
        if (destination == null || !destination.startsWith(APP_PREFIX)) {
            throw new AccessDenied("Destination không được phép: " + destination);
        }
    }

    /** Exception nhỏ để code gọn; kế thừa AccessDeniedException của Spring Security. */
    private static class AccessDenied extends AccessDeniedException {
        AccessDenied(String msg) {
            super(msg);
        }
    }
}