package com.tcgshop.service.impl;

import com.tcgshop.dto.request.BlockUserRequest;
import com.tcgshop.dto.request.UserUpdateRequest;
import com.tcgshop.dto.request.ToggleUserStatusRequest;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.UserManagementMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.UserManagementService;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final ShopRepository shopRepository;
	private final UserManagementMapper userManagementMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<UserManagementResponse> search(
			String keyword, RoleName roleName, UserStatus status, Pageable pageable) {
		boolean excludePrivileged = !isAdmin();
		var usersPage = userRepository.searchByKeywordAndStatusAndRole(
				normalizeKeyword(keyword),
				status,
				roleName,
				excludePrivileged,
				RoleName.ROLE_ADMIN,
				RoleName.ROLE_MANAGER,
				pageable);
		if (usersPage.isEmpty()) {
			return PageResponse.from(usersPage, userManagementMapper::toResponse);
		}

		Map<Long, User> usersWithRoles = userRepository.findAllWithRolesByIdIn(
						usersPage.map(User::getId).getContent())
				.stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		return PageResponse.from(usersPage, user -> {
			User userWithRoles = usersWithRoles.get(user.getId());
			if (userWithRoles == null) {
				throw new ResourceNotFoundException("Không tìm thấy người dùng trong kết quả tìm kiếm");
			}
			return userManagementMapper.toResponse(userWithRoles);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public UserManagementResponse getById(Long userId) {
		User user = findById(userId);
		ensureTargetIsManageable(user);
		return userManagementMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserManagementResponse block(Long userId, BlockUserRequest request) {
		User user = findById(userId);
		ensureTargetIsManageable(user);
		ensureNotSelf(user);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ConflictException("Chỉ có thể khóa tài khoản đang hoạt động");
		}

		user.setStatus(UserStatus.BLOCKED);
		user.setBlockReason(request.getReason().trim());
		user.setLockedUntil(null);
		return userManagementMapper.toResponse(userRepository.save(user));
	}

	@Override
	@Transactional
	public UserManagementResponse unblock(Long userId) {
		User user = findById(userId);
		ensureTargetIsManageable(user);
		ensureNotSelf(user);
		if (user.getStatus() != UserStatus.BLOCKED) {
			throw new ConflictException("Tài khoản hiện không bị khóa");
		}

		user.setStatus(UserStatus.ACTIVE);
		user.setBlockReason(null);
		user.setLockedUntil(null);
		user.setFailedLoginAttempts(0);
		return userManagementMapper.toResponse(userRepository.save(user));
	}

	private User findById(Long userId) {
		return userRepository.findByIdWithRoles(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
	}

	@Override
	@Transactional
	public UserManagementResponse update(Long userId, UserUpdateRequest request) {
		User user = findById(userId);
		ensureTargetIsManageable(user);
		if (request.getRole() != null) {
			if (!isAdmin()) {
				throw new ForbiddenException("Chỉ Admin được thay đổi vai trò người dùng");
			}
			if (user.getId().equals(currentUser.getId())) {
				throw new ForbiddenException("Admin không thể thay đổi vai trò của chính mình");
			}
			updateRoles(user, request.getRole());
		}
		user.setFullName(request.getFullName().trim());
		user.setPhoneNumber(request.getPhoneNumber().trim());
		return userManagementMapper.toResponse(userRepository.save(user));
	}

	private void updateRoles(User user, RoleName selectedRole) {
		if (selectedRole != RoleName.ROLE_VENDOR && hasRole(user, RoleName.ROLE_VENDOR)
				&& shopRepository.findByOwnerId(user.getId())
					.filter(shop -> !Boolean.TRUE.equals(shop.getDeleted())).isPresent()) {
			throw new ConflictException("Không thể bỏ vai trò Vendor: tài khoản này vẫn sở hữu Shop. Hãy xử lý Shop trước.");
		}

		Set<RoleName> names = new HashSet<>();
		names.add(selectedRole);
		// SECURITY_SPEC yêu cầu Vendor luôn có cả quyền mua hàng và quyền quản lý shop.
		if (selectedRole == RoleName.ROLE_VENDOR) names.add(RoleName.ROLE_USER);

		Set<com.tcgshop.entity.Role> roles = new HashSet<>();
		for (RoleName roleName : names) {
			roles.add(roleRepository.findByName(roleName)
					.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + roleName)));
		}
		user.setRoles(roles);
	}

	@Override
	@Transactional
	public UserManagementResponse toggleStatus(Long userId, ToggleUserStatusRequest request) {
		User user = findById(userId);
		ensureTargetIsManageable(user);
		ensureNotSelf(user);
		if (user.getStatus() != request.expectedStatus()) {
			throw new ConflictException("Trạng thái tài khoản đã thay đổi, vui lòng tải lại trang");
		}
		// INACTIVE chưa xác thực OTP; không dùng thao tác mở khóa để kích hoạt tài khoản.
		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new ConflictException("Tài khoản cần xác thực OTP trước khi quản lý khóa");
		}
		if (user.getStatus() == UserStatus.ACTIVE) {
			if (request.reason() == null || request.reason().isBlank() || request.reason().length() > 1000) {
				throw new BadRequestException("Vui lòng nhập lý do khóa từ 1 đến 1000 ký tự");
			}
			BlockUserRequest blockRequest = new BlockUserRequest();
			blockRequest.setReason(request.reason());
			return block(userId, blockRequest);
		}
		return unblock(userId);
	}

	private void ensureTargetIsManageable(User target) {
		if (!isAdmin() && (hasRole(target, RoleName.ROLE_ADMIN)
				|| hasRole(target, RoleName.ROLE_MANAGER))) {
			throw new ForbiddenException("Manager không có quyền quản lý tài khoản Admin hoặc Manager");
		}
	}

	private void ensureNotSelf(User target) {
		if (target.getId().equals(currentUser.getId())) {
			throw new ForbiddenException("Bạn không thể tự khóa hoặc mở khóa tài khoản của mình");
		}
	}

	private boolean isAdmin() {
		return currentUser.hasRole(RoleName.ROLE_ADMIN);
	}

	private boolean hasRole(User user, RoleName roleName) {
		return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}
