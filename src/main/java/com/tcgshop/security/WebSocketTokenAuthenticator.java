package com.tcgshop.security;

import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketTokenAuthenticator {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    public Optional<Authentication> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            if (!jwtProvider.validateToken(token)) {
                return Optional.empty();
            }
            String username = jwtProvider.getEmail(token);

            // Nạp lại user từ DB để lấy authorities (ROLE_*) mới nhất và kiểm tra tài khoản còn hoạt động
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                return Optional.empty();
            }
            return Optional.of(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
        } catch (Exception ex) {
            // Không ném lỗi ra ngoài, không log token. Chỉ log mức debug.
            log.debug("WebSocket: xác thực JWT thất bại: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}