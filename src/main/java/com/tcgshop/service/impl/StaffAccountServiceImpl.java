package com.tcgshop.service.impl;

import com.tcgshop.dto.request.StaffAccountRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.StaffAccountResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.StaffAccountMapper;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.EmailService;
import com.tcgshop.service.StaffAccountService;
import java.security.SecureRandom;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class StaffAccountServiceImpl implements StaffAccountService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS = "0123456789".toCharArray();
	private static final char[] ALL_CHARACTERS =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final OrderRepository orderRepository;
	private final StaffAccountMapper staffAccountMapper;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final CurrentUser currentUser;

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public StaffAccountResponse createManager(StaffAccountRequest request) {
		ensureRole(RoleName.ROLE_ADMIN);
		return createAccount(request, RoleName.ROLE_MANAGER);
	}

	@Override
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	@Transactional
	public StaffAccountResponse createShipper(StaffAccountRequest request) {
		ensureManagerOrAdmin();
		return createAccount(request, RoleName.ROLE_SHIPPER);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<StaffAccountResponse> searchShippers(
			String keyword, UserStatus status, Pageable pageable) {
		ensureManagerOrAdmin();
		var shippers = userRepository.searchByKeywordAndStatusAndRole(
				normalizeKeyword(keyword),
				status,
				RoleName.ROLE_SHIPPER,
				false,
				RoleName.ROLE_ADMIN,
				RoleName.ROLE_MANAGER,
				pageable);
		return PageResponse.from(shippers, shipper -> staffAccountMapper.toResponse(
				shipper,
				orderRepository.countByShipper_IdAndStatus(shipper.getId(), OrderStatus.SHIPPING)));
	}

	private StaffAccountResponse createAccount(StaffAccountRequest request, RoleName roleName) {
		if (request == null) {
			throw new BadRequestException("Thông tin tài khoản không được để trống");
		}
		String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email này đã được sử dụng");
		}

		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò cần cấp"));
		String temporaryPassword = generateTemporaryPassword();
		User user = new User();
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(temporaryPassword));
		user.setFullName(request.getFullName().trim());
		user.setPhoneNumber(request.getPhoneNumber().trim());
		// TODO(PA-13): Persist the shipper region after the User schema adds a region field.
		user.setStatus(UserStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setMustChangePassword(true);
		user.setRoles(Set.of(role));
		User savedUser = userRepository.save(user);

		emailService.sendTemporaryPasswordEmail(
				savedUser.getEmail(), savedUser.getFullName(), temporaryPassword, roleName.getDisplayName());
		return staffAccountMapper.toResponse(savedUser, 0);
	}

	private String generateTemporaryPassword() {
		char[] password = new char[12];
		password[0] = randomCharacter(UPPERCASE);
		password[1] = randomCharacter(LOWERCASE);
		password[2] = randomCharacter(DIGITS);
		for (int index = 3; index < password.length; index++) {
			password[index] = randomCharacter(ALL_CHARACTERS);
		}
		for (int index = password.length - 1; index > 0; index--) {
			int swapIndex = SECURE_RANDOM.nextInt(index + 1);
			char current = password[index];
			password[index] = password[swapIndex];
			password[swapIndex] = current;
		}
		return new String(password);
	}

	private char randomCharacter(char[] source) {
		return source[SECURE_RANDOM.nextInt(source.length)];
	}

	private void ensureRole(RoleName requiredRole) {
		if (!currentUser.hasRole(requiredRole)) {
			throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
		}
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER)
				&& !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
		}
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}
