package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.config.JwtProperties;
import com.tcgshop.entity.RefreshToken;
import com.tcgshop.entity.User;
import com.tcgshop.repository.RefreshTokenRepository;
import com.tcgshop.service.InvalidRefreshTokenException;
import com.tcgshop.service.RefreshTokenReuseException;
import com.tcgshop.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {
	private static final String RAW_TOKEN = "existing-refresh-token";

	@Mock private RefreshTokenRepository refreshTokenRepository;
	@Mock private JwtProperties jwtProperties;
	@InjectMocks private RefreshTokenServiceImpl refreshTokenService;

	@Test
	void rotateRevokesOldTokenAndIssuesHashedReplacement() throws Exception {
		when(jwtProperties.refreshExpirationDays()).thenReturn(7L);
		RefreshToken current = tokenFor(user(12L, "buyer@example.com"), false,
				LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
				.thenReturn(Optional.of(current));
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.Rotation rotation = refreshTokenService.rotate(RAW_TOKEN);

		assertTrue(current.isRevoked());
		assertEquals(12L, rotation.userId());
		assertEquals("buyer@example.com", rotation.email());
		assertEquals(32, Base64.getUrlDecoder().decode(rotation.rawToken()).length);
		assertNotEquals(RAW_TOKEN, rotation.rawToken());
		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(saved.capture());
		RefreshToken replacement = saved.getAllValues().get(1);
		assertEquals(hash(rotation.rawToken()), replacement.getTokenHash());
		assertFalse(replacement.isRevoked());
		assertTrue(replacement.getExpiresAt().isAfter(LocalDateTime.now()));
	}

	@Test
	void rotateRejectsExpiredToken() {
		RefreshToken expired = tokenFor(user(12L, "buyer@example.com"), false,
				LocalDateTime.now().minusSeconds(1));
		when(refreshTokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
				.thenReturn(Optional.of(expired));

		assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(RAW_TOKEN));
		verify(refreshTokenRepository, never()).revokeAllByUserId(any());
	}

	@Test
	void reusingRevokedTokenRevokesEveryTokenForItsUser() {
		RefreshToken revoked = tokenFor(user(12L, "buyer@example.com"), true,
				LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
				.thenReturn(Optional.of(revoked));
		when(refreshTokenRepository.revokeAllByUserId(12L)).thenReturn(3);

		assertThrows(RefreshTokenReuseException.class, () -> refreshTokenService.rotate(RAW_TOKEN));
		verify(refreshTokenRepository).revokeAllByUserId(12L);
	}

	@Test
	void rotateRejectsUnknownToken() {
		when(refreshTokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN))).thenReturn(Optional.empty());

		assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(RAW_TOKEN));
		verify(refreshTokenRepository, never()).revokeAllByUserId(any());
	}

	private RefreshToken tokenFor(User user, boolean revoked, LocalDateTime expiresAt) {
		RefreshToken token = new RefreshToken();
		token.setUser(user);
		token.setTokenHash(hash(RAW_TOKEN));
		token.setRevoked(revoked);
		token.setExpiresAt(expiresAt);
		return token;
	}

	private User user(Long id, String email) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		return user;
	}

	private String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}
}
