package com.tcgshop.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tcgshop.config.OtpProperties;
import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.event.OtpIssuedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.repository.OtpTokenQueryRepository;
import com.tcgshop.repository.OtpTokenRepository;
import com.tcgshop.service.OtpService;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OtpServiceImpl implements OtpService {
	private static final String INVALID_OTP_MESSAGE = "Mã OTP không hợp lệ hoặc đã hết hạn";

	private final OtpTokenRepository otpTokenRepository;
	private final OtpTokenQueryRepository otpTokenQueryRepository;
	private final PasswordEncoder passwordEncoder;
	private final OtpProperties otpProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final SecureRandom secureRandom;

	@Autowired
	public OtpServiceImpl(
			OtpTokenRepository otpTokenRepository,
			OtpTokenQueryRepository otpTokenQueryRepository,
			PasswordEncoder passwordEncoder,
			OtpProperties otpProperties,
			ApplicationEventPublisher eventPublisher) {
		this(otpTokenRepository, otpTokenQueryRepository, passwordEncoder, otpProperties, eventPublisher,
				new SecureRandom());
	}

	OtpServiceImpl(
			OtpTokenRepository otpTokenRepository,
			OtpTokenQueryRepository otpTokenQueryRepository,
			PasswordEncoder passwordEncoder,
			OtpProperties otpProperties,
			ApplicationEventPublisher eventPublisher,
			SecureRandom secureRandom) {
		this.otpTokenRepository = otpTokenRepository;
		this.otpTokenQueryRepository = otpTokenQueryRepository;
		this.passwordEncoder = passwordEncoder;
		this.otpProperties = otpProperties;
		this.eventPublisher = eventPublisher;
		this.secureRandom = secureRandom;
	}

	@Override
	@Transactional
	public void issue(User user, OtpPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();
		Optional<OtpToken> latestToken = otpTokenRepository
				.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
		if (latestToken.isPresent()) {
			LocalDateTime nextAllowedAt = latestToken.get().getCreatedAt()
					.plusSeconds(otpProperties.resendCooldownSeconds());
			if (now.isBefore(nextAllowedAt)) {
				long secondsRemaining = Math.max(1, Duration.between(now, nextAllowedAt).getSeconds());
				throw new BusinessException("Vui lòng đợi " + secondsRemaining + " giây trước khi gửi lại mã");
			}
		}

		List<OtpToken> unusedTokens = otpTokenQueryRepository
				.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose);
		unusedTokens.forEach(token -> token.setUsed(true));
		if (!unusedTokens.isEmpty()) {
			otpTokenRepository.saveAll(unusedTokens);
		}

		String otp = generateOtp(otpProperties.length());
		OtpToken token = new OtpToken();
		token.setUser(user);
		token.setPurpose(purpose);
		// OTP lưu bằng BCrypt như mật khẩu, không lưu bản rõ để hạn chế thiệt hại nếu DB bị lộ.
		token.setOtpHash(passwordEncoder.encode(otp));
		token.setExpiresAt(now.plusMinutes(otpProperties.validMinutes()));
		token.setAttemptCount(0);
		token.setUsed(false);
		otpTokenRepository.save(token);

		eventPublisher.publishEvent(new OtpIssuedEvent(
				user.getEmail(), user.getFullName(), otp, purpose, otpProperties.validMinutes()));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BadRequestException.class)
	public void verify(User user, OtpPurpose purpose, String otp) {
		/*
		 * REQUIRES_NEW + noRollbackFor bảo đảm attemptCount đã tăng vẫn được commit khi trả lỗi,
		 * nếu không người dùng có thể thử OTP vô hạn vì lần cập nhật bị rollback cùng exception.
		 */
		List<OtpToken> tokens = otpTokenQueryRepository
				.findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose);
		Optional<OtpToken> latestUnused = tokens.stream().findFirst();
		if (latestUnused.isEmpty()) {
			throw new BadRequestException(INVALID_OTP_MESSAGE);
		}

		OtpToken token = latestUnused.get();
		LocalDateTime now = LocalDateTime.now();
		if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
			throw new BadRequestException(INVALID_OTP_MESSAGE);
		}
		if (token.getAttemptCount() >= otpProperties.maxAttempts()) {
			token.setUsed(true);
			otpTokenRepository.save(token);
			throw new BadRequestException("Bạn đã nhập sai quá số lần cho phép, vui lòng yêu cầu mã mới");
		}

		if (!passwordEncoder.matches(otp, token.getOtpHash())) {
			token.setAttemptCount(token.getAttemptCount() + 1);
			otpTokenRepository.save(token);
			int attemptsRemaining = Math.max(0, otpProperties.maxAttempts() - token.getAttemptCount());
			throw new BadRequestException("Mã OTP không đúng. Còn " + attemptsRemaining + " lần thử");
		}

		token.setUsed(true);
		otpTokenRepository.save(token);
	}

	private String generateOtp(int length) {
		StringBuilder otp = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			otp.append(secureRandom.nextInt(10));
		}
		return otp.toString();
	}
}
