package com.tcgshop.service.impl;

import com.tcgshop.config.JwtProperties;
import com.tcgshop.entity.RefreshToken;
import com.tcgshop.entity.User;
import com.tcgshop.repository.RefreshTokenRepository;
import com.tcgshop.service.InvalidRefreshTokenException;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.RefreshTokenReuseException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final HexFormat HEX_FORMAT = HexFormat.of();

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtProperties = jwtProperties;
	}

	@Override
	@Transactional
	public String issue(User user) {
		String rawToken = generateRawToken();
		RefreshToken entity = new RefreshToken();
		entity.setUser(user);
		entity.setTokenHash(hash(rawToken));
		entity.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshExpirationDays()));
		entity.setRevoked(false);
		refreshTokenRepository.save(entity);
		return rawToken;
	}

	/**
	 * Khóa dòng token trong lúc xoay vòng để hai request đồng thời không cùng dùng một token.
	 * Khi token cũ bị gửi lại, việc thu hồi mọi phiên được commit dù ném exception; nếu không
	 * cấu hình noRollbackFor thì transaction sẽ rollback và dấu hiệu token bị đánh cắp mất đi.
	 */
	@Override
	@Transactional(noRollbackFor = RefreshTokenReuseException.class)
	public Rotation rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidRefreshTokenException("Refresh token is missing");
		}
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
				.orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

		if (current.isRevoked()) {
			refreshTokenRepository.revokeAllByUserId(current.getUser().getId());
			throw new RefreshTokenReuseException();
		}
		if (current.getExpiresAt() == null || !current.getExpiresAt().isAfter(LocalDateTime.now())) {
			throw new InvalidRefreshTokenException("Refresh token has expired");
		}

		User user = current.getUser();
		current.setRevoked(true);
		String nextRawToken = generateRawToken();
		RefreshToken next = new RefreshToken();
		next.setUser(user);
		next.setTokenHash(hash(nextRawToken));
		next.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshExpirationDays()));
		next.setRevoked(false);
		refreshTokenRepository.save(current);
		refreshTokenRepository.save(next);
		return new Rotation(nextRawToken, user.getId(), user.getEmail());
	}

	@Override
	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) return;
		refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
				.ifPresent(token -> token.setRevoked(true));
	}

	@Override
	@Transactional
	public void revokeAllByUser(Long userId) {
		if (userId != null) refreshTokenRepository.revokeAllByUserId(userId);
	}

	/** Xóa vật lý token đã hết hạn mỗi ngày; token còn hạn vẫn giữ để phát hiện phát lại. */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanupExpiredTokens() {
		refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());
	}

	private static String generateRawToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HEX_FORMAT.formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
