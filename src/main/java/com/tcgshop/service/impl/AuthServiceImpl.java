package com.tcgshop.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.request.ChangePasswordRequest;
import com.tcgshop.dto.request.ForgotPasswordRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.ResetPasswordRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;
import com.tcgshop.dto.response.AuthTokensResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.AccountLockedException;
import com.tcgshop.exception.AccountNotActivatedException;
import com.tcgshop.exception.AuthenticationFailedException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.AuthService;
import com.tcgshop.service.EmailService;
import com.tcgshop.service.LoginAttemptService;
import com.tcgshop.service.OtpService;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.service.InvalidRefreshTokenException;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final String INVALID_OTP_MESSAGE = "Mã OTP không hợp lệ hoặc đã hết hạn";
	private static final String PASSWORD_RESET_INVALID_OTP_MESSAGE =
			"Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã";
	private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final OtpService otpService;
	private final LoginAttemptService loginAttemptService;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;
	private final CurrentUser currentUser;
	private final EmailService emailService;

	public AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			OtpService otpService,
			LoginAttemptService loginAttemptService,
			JwtProvider jwtProvider,
			RefreshTokenService refreshTokenService,
			CurrentUser currentUser,
			EmailService emailService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.otpService = otpService;
		this.loginAttemptService = loginAttemptService;
		this.jwtProvider = jwtProvider;
		this.refreshTokenService = refreshTokenService;
		this.currentUser = currentUser;
		this.emailService = emailService;
	}

	@Override
	public void register(RegisterRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmail(email).orElse(null);

		if (user != null && user.getStatus() == UserStatus.ACTIVE) {
			throw new ConflictException("Email đã được sử dụng");
		}
		// Tài khoản BLOCKED không được biến thành tài khoản đăng ký dở khi gửi lại biểu mẫu.
		if (user != null && user.getStatus() != UserStatus.INACTIVE) {
			throw new ConflictException("Email đã được sử dụng");
		}

		Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
				.orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.ROLE_USER));
		if (user == null) {
			user = new User();
			user.setEmail(email);
			user.setStatus(UserStatus.INACTIVE);
			user.setEmailVerified(false);
		}
		user.getRoles().add(userRole);

		user.setFullName(request.getFullName().trim());
		user.setPhoneNumber(request.getPhoneNumber().trim());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		userRepository.save(user);
		otpService.issue(user, OtpPurpose.REGISTER);
	}

	@Override
	public void verifyRegistration(VerifyOtpRequest request) {
		User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
				.filter(candidate -> candidate.getStatus() == UserStatus.INACTIVE)
				.orElseThrow(() -> new BadRequestException(INVALID_OTP_MESSAGE));

		// Xác minh trước khi sửa User: OtpService.verify mở REQUIRES_NEW, tránh xung đột khóa hàng User.
		otpService.verify(user, OtpPurpose.REGISTER, request.getOtp());
		user.setStatus(UserStatus.ACTIVE);
		user.setEmailVerified(true);
		userRepository.save(user);
	}

	@Override
	public void resendRegistrationOtp(ResendOtpRequest request) {
		userRepository.findByEmail(normalizeEmail(request.getEmail()))
				.filter(user -> user.getStatus() == UserStatus.INACTIVE)
				.ifPresent(user -> otpService.issue(user, OtpPurpose.REGISTER));
	}

	/**
	 * Ghi nhận lỗi qua bean REQUIRES_NEW trước khi ném exception; noRollbackFor cũng giữ transaction
	 * đăng nhập không bị đánh dấu rollback-only. Tài khoản bị chặn chỉ nhận thông báo trạng thái sau
	 * khi mật khẩu đúng, để không tiết lộ trạng thái tài khoản cho người ngoài.
	 */
	@Override
	@Transactional(noRollbackFor = {AuthenticationFailedException.class,
			AccountLockedException.class, AccountNotActivatedException.class})
	public AuthTokensResponse login(LoginRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			// Hash giả giúp nhánh email không tồn tại tốn thời gian BCrypt gần bằng nhánh kiểm tra thật.
			passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
			throw new AuthenticationFailedException();
		}

		LocalDateTime now = LocalDateTime.now();
		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
			long secondsLeft = Duration.between(now, user.getLockedUntil()).getSeconds();
			long minutesLeft = Math.max(1, (secondsLeft + 59) / 60);
			throw new AccountLockedException("Tài khoản tạm thời bị khóa do nhập sai nhiều lần, vui lòng thử lại sau "
					+ minutesLeft + " phút");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			loginAttemptService.recordFailure(email);
			throw new AuthenticationFailedException();
		}
		if (user.getStatus() == UserStatus.BLOCKED) {
			throw new AccountLockedException("Tài khoản của bạn đã bị khóa, vui lòng liên hệ quản trị viên");
		}
		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new AccountNotActivatedException(user.getEmail());
		}

		loginAttemptService.recordSuccess(user.getId());
		return createTokens(user);
	}

	@Override
	public void logout(String refreshTokenRaw) {
		if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) return;
		try {
			refreshTokenService.revoke(refreshTokenRaw);
		} catch (InvalidRefreshTokenException ignored) {
			// Logout là idempotent; token hết hạn/không hợp lệ không cần báo lỗi cho người dùng.
		}
	}

	@Override
	public AuthTokensResponse changePassword(ChangePasswordRequest request) {
		User user = userRepository.findById(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new BadRequestException("Mật khẩu hiện tại không đúng");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new BadRequestException("Mật khẩu mới phải khác mật khẩu cũ");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		user.setMustChangePassword(false);
		userRepository.save(user);
		refreshTokenService.revokeAllByUser(user.getId());
		return createTokens(user);
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void requestPasswordReset(ForgotPasswordRequest request) {
		// Luôn trả về bình thường để người gửi không thể dò email qua trạng thái hay thời gian phản hồi.
		String email = normalizeEmail(request.getEmail());
		userRepository.findByEmail(email)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.ifPresent(user -> {
					try {
						otpService.issue(user, OtpPurpose.FORGOT_PASSWORD);
					} catch (BusinessException exception) {
						// Email không được ghi log; phản hồi giống nhau để không tiết lộ tài khoản hay cooldown.
						log.debug("Password reset OTP request suppressed by resend cooldown");
					}
				});
	}

	@Override
	public void resetPassword(ResetPasswordRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmail(email)
				.filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new BadRequestException(PASSWORD_RESET_INVALID_OTP_MESSAGE));

		try {
			// Verify phải hoàn tất REQUIRES_NEW trước khi thay đổi User để lỗi OTP vẫn lưu attemptCount.
			otpService.verify(user, OtpPurpose.FORGOT_PASSWORD, request.getOtp());
		} catch (BadRequestException exception) {
			// Dùng cùng thông điệp cho mọi lỗi OTP để không lộ trạng thái tài khoản hoặc số lần thử còn lại.
			throw new BadRequestException(PASSWORD_RESET_INVALID_OTP_MESSAGE);
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		user.setMustChangePassword(false);
		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		userRepository.save(user);
		refreshTokenService.revokeAllByUser(user.getId());
		emailService.sendNotificationEmail(
				user.getEmail(),
				"Mật khẩu của bạn vừa được thay đổi",
				"Mật khẩu của bạn vừa được thay đổi",
				List.of("Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ bộ phận hỗ trợ ngay."));
		// Access token đã cấp trước đó vẫn có hiệu lực đến khi hết hạn (tối đa 30 phút).
	}

	private AuthTokensResponse createTokens(User user) {
		CustomUserDetails userDetails = new CustomUserDetails(user);
		String accessToken = jwtProvider.generateAccessToken(userDetails);
		String refreshToken = refreshTokenService.issue(user);
		Set<RoleName> roles = user.getRoles().stream()
				.map(Role::getName)
				.collect(Collectors.toUnmodifiableSet());
		return new AuthTokensResponse(accessToken, refreshToken, roles, user.isMustChangePassword());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
