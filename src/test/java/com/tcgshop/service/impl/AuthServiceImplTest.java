package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.LoginRequest;
import com.tcgshop.dto.request.ChangePasswordRequest;
import com.tcgshop.dto.request.ForgotPasswordRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.ResetPasswordRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.AccountLockedException;
import com.tcgshop.exception.AccountNotActivatedException;
import com.tcgshop.exception.AuthenticationFailedException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.service.EmailService;
import com.tcgshop.service.LoginAttemptService;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.OtpService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private OtpService otpService;
	@Mock
	private LoginAttemptService loginAttemptService;
	@Mock
	private JwtProvider jwtProvider;
	@Mock
	private RefreshTokenService refreshTokenService;
	@Mock
	private CurrentUser currentUser;
	@Mock
	private EmailService emailService;

	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, otpService,
				loginAttemptService, jwtProvider, refreshTokenService, currentUser, emailService);
	}

	@Test
	void loginWithValidActiveCredentialsIssuesBothTokens() {
		User user = activeUser();
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("CorrectPass1", "stored-hash")).thenReturn(true);
		when(jwtProvider.generateAccessToken(any())).thenReturn("access-token");
		when(refreshTokenService.issue(user)).thenReturn("refresh-token");

		var result = authService.login(loginRequest(" Buyer@Example.com ", "CorrectPass1"));

		assertEquals("access-token", result.accessToken());
		assertEquals("refresh-token", result.refreshToken());
		assertEquals(Set.of(RoleName.ROLE_USER), result.roles());
		verify(loginAttemptService).recordSuccess(7L);
	}

	@Test
	void loginForMissingEmailRunsDummyBcryptAndUsesGenericFailure() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(AuthenticationFailedException.class,
				() -> authService.login(loginRequest("missing@example.com", "anything")));

		verify(passwordEncoder).matches(eq("anything"), any(String.class));
		verify(loginAttemptService, never()).recordFailure(any());
	}

	@Test
	void wrongPasswordRecordsFailureThenReturnsGenericFailure() {
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(activeUser()));
		when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

		assertThrows(AuthenticationFailedException.class,
				() -> authService.login(loginRequest("buyer@example.com", "wrong")));

		verify(loginAttemptService).recordFailure("buyer@example.com");
	}

	@Test
	void activeLockDoesNotCheckPassword() {
		User user = activeUser();
		user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(3));
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

		assertThrows(AccountLockedException.class,
				() -> authService.login(loginRequest("buyer@example.com", "anything")));

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void blockedAccountWithWrongPasswordStillGetsGenericFailure() {
		User user = activeUser();
		user.setStatus(UserStatus.BLOCKED);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

		assertEquals("Email hoặc mật khẩu không đúng", assertThrows(AuthenticationFailedException.class,
				() -> authService.login(loginRequest("buyer@example.com", "wrong"))).getMessage());
	}

	@Test
	void blockedAndInactiveAccountsAreReportedOnlyAfterCorrectPassword() {
		User blocked = activeUser();
		blocked.setStatus(UserStatus.BLOCKED);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(blocked));
		when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);

		assertEquals("Tài khoản của bạn đã bị khóa, vui lòng liên hệ quản trị viên",
				assertThrows(AccountLockedException.class,
						() -> authService.login(loginRequest("buyer@example.com", "correct"))).getMessage());

		User inactive = activeUser();
		inactive.setStatus(UserStatus.INACTIVE);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(inactive));
		AccountNotActivatedException notActivated = assertThrows(AccountNotActivatedException.class,
				() -> authService.login(loginRequest("buyer@example.com", "correct")));
		assertEquals("buyer@example.com", notActivated.getEmail());
	}

	@Test
	void loginResponseCarriesMustChangePasswordFlag() {
		User user = activeUser();
		user.setMustChangePassword(true);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("CorrectPass1", "stored-hash")).thenReturn(true);
		when(jwtProvider.generateAccessToken(any())).thenReturn("access-token");
		when(refreshTokenService.issue(user)).thenReturn("refresh-token");

		assertTrue(authService.login(loginRequest("buyer@example.com", "CorrectPass1")).mustChangePassword());
	}

	@Test
	void changePasswordValidatesCurrentAndDifferentPasswordThenRotatesTokens() {
		User user = activeUser();
		user.setMustChangePassword(true);
		when(currentUser.getId()).thenReturn(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("old", "stored-hash")).thenReturn(true);
		when(passwordEncoder.matches("new", "stored-hash")).thenReturn(false);
		when(passwordEncoder.encode("new")).thenReturn("new-hash");
		when(jwtProvider.generateAccessToken(any())).thenReturn("fresh-access");
		when(refreshTokenService.issue(user)).thenReturn("fresh-refresh");
		ChangePasswordRequest request = changePasswordRequest("old", "new");

		var result = authService.changePassword(request);

		assertEquals("new-hash", user.getPassword());
		assertFalse(user.isMustChangePassword());
		assertEquals("fresh-access", result.accessToken());
		assertEquals("fresh-refresh", result.refreshToken());
		verify(refreshTokenService).revokeAllByUser(7L);
	}

	@Test
	void changePasswordRejectsWrongCurrentOrReusedPassword() {
		User user = activeUser();
		when(currentUser.getId()).thenReturn(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);
		assertEquals("Mật khẩu hiện tại không đúng", assertThrows(BadRequestException.class,
				() -> authService.changePassword(changePasswordRequest("wrong", "new"))).getMessage());

		when(passwordEncoder.matches("old", "stored-hash")).thenReturn(true);
		assertEquals("Mật khẩu mới phải khác mật khẩu cũ", assertThrows(BadRequestException.class,
				() -> authService.changePassword(changePasswordRequest("old", "old"))).getMessage());
	}

	@Test
	void registerCreatesInactiveUserWithUserRoleAndIssuesOtp() {
		RegisterRequest request = registerRequest("  Buyer@Example.com  ");
		Role userRole = new Role();
		userRole.setName(RoleName.ROLE_USER);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.empty());
		when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
		when(passwordEncoder.encode("StrongPass1")).thenReturn("bcrypt-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(request);

		org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User saved = userCaptor.getValue();
		assertEquals("buyer@example.com", saved.getEmail());
		assertEquals("bcrypt-hash", saved.getPassword());
		assertEquals(UserStatus.INACTIVE, saved.getStatus());
		assertFalse(saved.isEmailVerified());
		assertTrue(saved.getRoles().contains(userRole));
		verify(otpService).issue(saved, OtpPurpose.REGISTER);
	}

	@Test
	void registerRejectsEmailAlreadyOwnedByActiveUser() {
		User activeUser = new User();
		activeUser.setStatus(UserStatus.ACTIVE);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(activeUser));

		ConflictException exception = assertThrows(ConflictException.class,
				() -> authService.register(registerRequest("buyer@example.com")));

		assertEquals("Email đã được sử dụng", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
		verify(otpService, never()).issue(any(User.class), any(OtpPurpose.class));
	}

	@Test
	void registerUpdatesExistingInactiveAccountAndReissuesOtp() {
		User inactiveUser = new User();
		inactiveUser.setEmail("buyer@example.com");
		inactiveUser.setStatus(UserStatus.INACTIVE);
		Role userRole = new Role();
		userRole.setName(RoleName.ROLE_USER);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(inactiveUser));
		when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
		when(passwordEncoder.encode("StrongPass1")).thenReturn("new-bcrypt-hash");

		RegisterRequest request = registerRequest("buyer@example.com");
		authService.register(request);

		assertEquals("Buyer Name", inactiveUser.getFullName());
		assertEquals("0912345678", inactiveUser.getPhoneNumber());
		assertEquals("new-bcrypt-hash", inactiveUser.getPassword());
		assertTrue(inactiveUser.getRoles().contains(userRole));
		verify(userRepository).save(inactiveUser);
		verify(otpService).issue(inactiveUser, OtpPurpose.REGISTER);
		verify(roleRepository).findByName(RoleName.ROLE_USER);
	}

	@Test
	void verifyRegistrationVerifiesOtpBeforeChangingUserStatus() {
		User inactiveUser = new User();
		inactiveUser.setEmail("buyer@example.com");
		inactiveUser.setStatus(UserStatus.INACTIVE);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(inactiveUser));
		org.mockito.Mockito.doAnswer(invocation -> {
			assertEquals(UserStatus.INACTIVE, inactiveUser.getStatus());
			assertFalse(inactiveUser.isEmailVerified());
			return null;
		}).when(otpService).verify(inactiveUser, OtpPurpose.REGISTER, "001234");

		VerifyOtpRequest request = new VerifyOtpRequest();
		request.setEmail(" Buyer@Example.com ");
		request.setOtp("001234");
		authService.verifyRegistration(request);

		assertEquals(UserStatus.ACTIVE, inactiveUser.getStatus());
		assertTrue(inactiveUser.isEmailVerified());
		InOrder order = inOrder(otpService, userRepository);
		order.verify(otpService).verify(inactiveUser, OtpPurpose.REGISTER, "001234");
		order.verify(userRepository).save(inactiveUser);
	}

	@Test
	void verifyRegistrationUsesGenericMessageForUnknownOrNonInactiveAccount() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		VerifyOtpRequest request = new VerifyOtpRequest();
		request.setEmail("missing@example.com");
		request.setOtp("001234");

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> authService.verifyRegistration(request));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
		verify(otpService, never()).verify(any(User.class), any(OtpPurpose.class), any());
	}

	@Test
	void verifyRegistrationDoesNotActivateUserWhenOtpVerificationFails() {
		User inactiveUser = new User();
		inactiveUser.setEmail("buyer@example.com");
		inactiveUser.setStatus(UserStatus.INACTIVE);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(inactiveUser));
		org.mockito.Mockito.doThrow(new BadRequestException("Mã OTP không đúng"))
				.when(otpService).verify(inactiveUser, OtpPurpose.REGISTER, "999999");
		VerifyOtpRequest request = new VerifyOtpRequest();
		request.setEmail("buyer@example.com");
		request.setOtp("999999");

		assertThrows(BadRequestException.class, () -> authService.verifyRegistration(request));

		assertEquals(UserStatus.INACTIVE, inactiveUser.getStatus());
		assertFalse(inactiveUser.isEmailVerified());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void resendForUnknownEmailSucceedsWithoutIssuingOtp() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		ResendOtpRequest request = new ResendOtpRequest();
		request.setEmail("missing@example.com");

		authService.resendRegistrationOtp(request);

		verify(otpService, never()).issue(any(User.class), any(OtpPurpose.class));
	}

	@Test
	void requestPasswordResetIssuesOtpForActiveUserUsingNormalizedEmail() {
		User user = activeUser();
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

		authService.requestPasswordReset(forgotPasswordRequest(" Buyer@Example.com "));

		verify(otpService).issue(user, OtpPurpose.FORGOT_PASSWORD);
	}

	@Test
	void requestPasswordResetForUnknownEmailCompletesWithoutIssuingOtp() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> authService.requestPasswordReset(forgotPasswordRequest("missing@example.com")));

		verify(otpService, never()).issue(any(User.class), any(OtpPurpose.class));
	}

	@Test
	void requestPasswordResetDoesNotIssueOtpForInactiveOrBlockedUsers() {
		User inactive = activeUser();
		inactive.setStatus(UserStatus.INACTIVE);
		User blocked = activeUser();
		blocked.setStatus(UserStatus.BLOCKED);
		when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactive));
		when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(blocked));

		assertDoesNotThrow(() -> authService.requestPasswordReset(forgotPasswordRequest("inactive@example.com")));
		assertDoesNotThrow(() -> authService.requestPasswordReset(forgotPasswordRequest("blocked@example.com")));

		verify(otpService, never()).issue(any(User.class), any(OtpPurpose.class));
	}

	@Test
	void requestPasswordResetSuppressesResendCooldown() {
		User user = activeUser();
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		doThrow(new BusinessException("cooldown detail"))
				.when(otpService).issue(user, OtpPurpose.FORGOT_PASSWORD);

		assertDoesNotThrow(() -> authService.requestPasswordReset(forgotPasswordRequest("buyer@example.com")));

		verify(otpService).issue(user, OtpPurpose.FORGOT_PASSWORD);
	}

	@Test
	void resetPasswordUpdatesCredentialsRevokesRefreshTokensAndPublishesNotification() {
		User user = activeUser();
		user.setMustChangePassword(true);
		user.setFailedLoginAttempts(4);
		user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
		String storedHash = new BCryptPasswordEncoder().encode("NewStrongPass1");
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("NewStrongPass1")).thenReturn(storedHash);
		org.mockito.Mockito.doAnswer(invocation -> {
			assertEquals("stored-hash", user.getPassword());
			assertEquals(4, user.getFailedLoginAttempts());
			assertTrue(user.isMustChangePassword());
			assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
			return null;
		}).when(otpService).verify(user, OtpPurpose.FORGOT_PASSWORD, "001234");

		authService.resetPassword(resetPasswordRequest(" Buyer@Example.com ", "001234"));

		assertTrue(new BCryptPasswordEncoder().matches("NewStrongPass1", user.getPassword()));
		assertFalse(user.isMustChangePassword());
		assertEquals(0, user.getFailedLoginAttempts());
		assertNull(user.getLockedUntil());

		InOrder order = inOrder(otpService, passwordEncoder, userRepository, refreshTokenService, emailService);
		order.verify(otpService).verify(user, OtpPurpose.FORGOT_PASSWORD, "001234");
		order.verify(passwordEncoder).encode("NewStrongPass1");
		order.verify(userRepository).save(user);
		order.verify(refreshTokenService).revokeAllByUser(7L);
		order.verify(emailService).sendNotificationEmail(
				"buyer@example.com",
				"Mật khẩu của bạn vừa được thay đổi",
				"Mật khẩu của bạn vừa được thay đổi",
				java.util.List.of("Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ bộ phận hỗ trợ ngay."));
	}

	@Test
	void resetPasswordWithIncorrectOtpUsesGenericMessageAndDoesNotChangePassword() {
		User user = activeUser();
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		doThrow(new BadRequestException("Mã OTP không đúng. Còn 2 lần thử"))
				.when(otpService).verify(user, OtpPurpose.FORGOT_PASSWORD, "999999");

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> authService.resetPassword(resetPasswordRequest("buyer@example.com", "999999")));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã",
				exception.getMessage());
		assertEquals("stored-hash", user.getPassword());
		verify(userRepository, never()).save(any(User.class));
		verify(refreshTokenService, never()).revokeAllByUser(any());
		verify(emailService, never()).sendNotificationEmail(any(), any(), any(), any());
	}

	@Test
	void resetPasswordForUnknownEmailUsesSameGenericMessageAsIncorrectOtp() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		BadRequestException missingUserException = assertThrows(BadRequestException.class,
				() -> authService.resetPassword(resetPasswordRequest("missing@example.com", "001234")));

		User user = activeUser();
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
		doThrow(new BadRequestException("verify detail"))
				.when(otpService).verify(user, OtpPurpose.FORGOT_PASSWORD, "999999");
		BadRequestException incorrectOtpException = assertThrows(BadRequestException.class,
				() -> authService.resetPassword(resetPasswordRequest("buyer@example.com", "999999")));

		assertEquals(missingUserException.getMessage(), incorrectOtpException.getMessage());
		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã",
				missingUserException.getMessage());
		assertEquals("stored-hash", user.getPassword());
		verify(otpService).verify(user, OtpPurpose.FORGOT_PASSWORD, "999999");
	}

	@Test
	void resetPasswordForBlockedUserUsesGenericMessageWithoutChangingPassword() {
		User user = activeUser();
		user.setStatus(UserStatus.BLOCKED);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> authService.resetPassword(resetPasswordRequest("buyer@example.com", "001234")));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã",
				exception.getMessage());
		assertEquals("stored-hash", user.getPassword());
		verify(otpService, never()).verify(any(User.class), any(OtpPurpose.class), any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void resetPasswordDoesNotChangePasswordWhenUserIsInactive() {
		User user = activeUser();
		user.setStatus(UserStatus.INACTIVE);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> authService.resetPassword(resetPasswordRequest("buyer@example.com", "001234")));

		assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn. Nếu cần, hãy yêu cầu gửi lại mã",
				exception.getMessage());
		assertEquals("stored-hash", user.getPassword());
		verify(otpService, never()).verify(any(User.class), any(OtpPurpose.class), any());
	}

	private RegisterRequest registerRequest(String email) {
		RegisterRequest request = new RegisterRequest();
		request.setFullName(" Buyer Name ");
		request.setEmail(email);
		request.setPhoneNumber("0912345678");
		request.setPassword("StrongPass1");
		request.setConfirmPassword("StrongPass1");
		return request;
	}

	private ForgotPasswordRequest forgotPasswordRequest(String email) {
		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail(email);
		return request;
	}

	private ResetPasswordRequest resetPasswordRequest(String email, String otp) {
		ResetPasswordRequest request = new ResetPasswordRequest();
		request.setEmail(email);
		request.setOtp(otp);
		request.setNewPassword("NewStrongPass1");
		request.setConfirmPassword("NewStrongPass1");
		return request;
	}

	private User activeUser() {
		User user = new User();
		user.setId(7L);
		user.setEmail("buyer@example.com");
		user.setPassword("stored-hash");
		user.setStatus(UserStatus.ACTIVE);
		Role role = new Role();
		role.setName(RoleName.ROLE_USER);
		user.setRoles(new HashSet<>(Set.of(role)));
		return user;
	}

	private LoginRequest loginRequest(String email, String password) {
		LoginRequest request = new LoginRequest();
		request.setEmail(email);
		request.setPassword(password);
		return request;
	}

	private ChangePasswordRequest changePasswordRequest(String current, String next) {
		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword(current);
		request.setNewPassword(next);
		request.setConfirmPassword(next);
		return request;
	}
}
