package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tcgshop.config.OtpProperties;
import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.event.OtpIssuedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.repository.OtpTokenQueryRepository;
import com.tcgshop.repository.OtpTokenRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {
	@Mock
	private OtpTokenRepository otpTokenRepository;

	@Mock
	private OtpTokenQueryRepository otpTokenQueryRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private SecureRandom secureRandom;

	private OtpServiceImpl otpService;
	private User user;
	private OtpProperties properties;

	@BeforeEach
	void setUp() {
		properties = new OtpProperties(6, 5, 5, 60);
		user = new User();
		user.setId(42L);
		user.setEmail("member@example.com");
		user.setFullName("Nguyễn An");
		otpService = new OtpServiceImpl(otpTokenRepository, otpTokenQueryRepository,
				passwordEncoder, properties, eventPublisher, secureRandom);
	}

	@Test
	void issueCreatesHashedOtpAndPublishesEventWithLeadingZeros() {
		when(otpTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(Optional.empty());
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of());
		when(secureRandom.nextInt(10)).thenReturn(0);
		when(passwordEncoder.encode("000000")).thenReturn("bcrypt-hash");

		otpService.issue(user, OtpPurpose.REGISTER);

		ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
		verify(otpTokenRepository).save(tokenCaptor.capture());
		OtpToken saved = tokenCaptor.getValue();
		assertEquals("bcrypt-hash", saved.getOtpHash());
		assertEquals(0, saved.getAttemptCount());
		assertFalse(saved.isUsed());
		assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(4)));
		verify(eventPublisher).publishEvent(new OtpIssuedEvent(
				"member@example.com", "Nguyễn An", "000000", OtpPurpose.REGISTER, 5));
	}

	@Test
	void issueRejectsResendDuringCooldown() {
		OtpToken latest = token(LocalDateTime.now().minusSeconds(10), 0, false, LocalDateTime.now().plusMinutes(5));
		when(otpTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(Optional.of(latest));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> otpService.issue(user, OtpPurpose.REGISTER));

		assertTrue(exception.getMessage().startsWith("Vui lòng đợi "));
		verify(otpTokenRepository, never()).save(any(OtpToken.class));
	}

	@Test
	void issueInvalidatesAllOlderUnusedTokensBeforeSavingNewOne() {
		when(otpTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(Optional.of(token(LocalDateTime.now().minusMinutes(2), 0, false,
						LocalDateTime.now().plusMinutes(3))));
		OtpToken previousFirst = token(LocalDateTime.now().minusMinutes(2), 0, false, LocalDateTime.now().plusMinutes(3));
		OtpToken previousSecond = token(LocalDateTime.now().minusMinutes(3), 1, false, LocalDateTime.now().plusMinutes(2));
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of(previousFirst, previousSecond));
		when(secureRandom.nextInt(10)).thenReturn(0);
		when(passwordEncoder.encode("000000")).thenReturn("bcrypt-hash");

		otpService.issue(user, OtpPurpose.REGISTER);

		assertTrue(previousFirst.isUsed());
		assertTrue(previousSecond.isUsed());
		verify(otpTokenRepository).saveAll(List.of(previousFirst, previousSecond));
		verify(otpTokenRepository).save(any(OtpToken.class));
	}

	@Test
	void verifyMarksMatchingOtpUsed() {
		OtpToken token = token(LocalDateTime.now().minusSeconds(1), 0, false, LocalDateTime.now().plusMinutes(5));
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of(token));
		when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
		token.setOtpHash("hash");

		otpService.verify(user, OtpPurpose.REGISTER, "123456");

		assertTrue(token.isUsed());
		verify(otpTokenRepository).save(token);
	}

	@Test
	void verifyWrongOtpIncrementsAndSavesAttemptCount() {
		OtpToken token = token(LocalDateTime.now().minusSeconds(1), 1, false, LocalDateTime.now().plusMinutes(5));
		token.setOtpHash("hash");
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of(token));
		when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> otpService.verify(user, OtpPurpose.REGISTER, "bad"));

		assertEquals(2, token.getAttemptCount());
		assertTrue(exception.getMessage().contains("Còn 3 lần thử"));
		verify(otpTokenRepository).save(token);
	}

	@Test
	void verifyLocksTokenWhenAttemptLimitWasAlreadyReached() {
		OtpToken token = token(LocalDateTime.now().minusSeconds(1), 5, false, LocalDateTime.now().plusMinutes(5));
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of(token));

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"));

		assertTrue(token.isUsed());
		assertTrue(exception.getMessage().contains("nhập sai quá số lần"));
		verify(otpTokenRepository).save(token);
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void verifyRejectsExpiredOtpWithGenericMessage() {
		OtpToken token = token(LocalDateTime.now().minusSeconds(1), 0, false, LocalDateTime.now().minusSeconds(1));
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of(token));

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void verifyRejectsUsedOtpAsUnavailable() {
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of());

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
	}

	@Test
	void verifyRejectsWhenNoOtpExists() {
		when(otpTokenQueryRepository.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(42L, OtpPurpose.REGISTER))
				.thenReturn(List.of());

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
	}

	private OtpToken token(LocalDateTime createdAt, int attempts, boolean used, LocalDateTime expiresAt) {
		OtpToken token = new OtpToken();
		token.setUser(user);
		token.setPurpose(OtpPurpose.REGISTER);
		token.setOtpHash("hash");
		token.setCreatedAt(createdAt);
		token.setExpiresAt(expiresAt);
		token.setAttemptCount(attempts);
		token.setUsed(used);
		return token;
	}
}
