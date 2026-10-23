package com.tcgshop.service.impl;

import com.tcgshop.dto.request.BlockUserRequest;
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
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.UserManagementService;
import java.util.Map;
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
