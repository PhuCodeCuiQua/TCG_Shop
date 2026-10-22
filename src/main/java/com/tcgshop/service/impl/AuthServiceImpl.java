package com.tcgshop.service.impl;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.AuthService;
import com.tcgshop.service.OtpService;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
	private static final String INVALID_OTP_MESSAGE = "Mã OTP không hợp lệ hoặc đã hết hạn";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final OtpService otpService;

	public AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			OtpService otpService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.otpService = otpService;
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

		if (user == null) {
			Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
					.orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.ROLE_USER));
			user = new User();
			user.setEmail(email);
			user.setStatus(UserStatus.INACTIVE);
			user.setEmailVerified(false);
			user.getRoles().add(userRole);
		}

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

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
