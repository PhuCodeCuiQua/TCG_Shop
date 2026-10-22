package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tcgshop.dto.request.RegisterRequest;
import com.tcgshop.dto.request.ResendOtpRequest;
import com.tcgshop.dto.request.VerifyOtpRequest;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OtpPurpose;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
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

	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, otpService);
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
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(inactiveUser));
		when(passwordEncoder.encode("StrongPass1")).thenReturn("new-bcrypt-hash");

		RegisterRequest request = registerRequest("buyer@example.com");
		authService.register(request);

		assertEquals("Buyer Name", inactiveUser.getFullName());
		assertEquals("0912345678", inactiveUser.getPhoneNumber());
		assertEquals("new-bcrypt-hash", inactiveUser.getPassword());
		verify(userRepository).save(inactiveUser);
		verify(otpService).issue(inactiveUser, OtpPurpose.REGISTER);
		verify(roleRepository, never()).findByName(any(RoleName.class));
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

	private RegisterRequest registerRequest(String email) {
		RegisterRequest request = new RegisterRequest();
		request.setFullName(" Buyer Name ");
		request.setEmail(email);
		request.setPhoneNumber("0912345678");
		request.setPassword("StrongPass1");
		request.setConfirmPassword("StrongPass1");
		return request;
	}
}
