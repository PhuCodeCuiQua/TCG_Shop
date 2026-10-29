package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.BlockUserRequest;
import com.tcgshop.dto.request.ToggleUserStatusRequest;
import com.tcgshop.dto.request.UserUpdateRequest;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.UserManagementMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

	private static final Long CURRENT_USER_ID = 10L;
	private static final Long TARGET_USER_ID = 20L;

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserManagementMapper userManagementMapper;
	@Mock
	private CurrentUser currentUser;
	@InjectMocks
	private UserManagementServiceImpl service;

	@Test
	void managerCannotLockAdmin() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
		User admin = user(TARGET_USER_ID, RoleName.ROLE_ADMIN, UserStatus.ACTIVE);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service.block(TARGET_USER_ID, request("Policy violation")))
				.isInstanceOf(ForbiddenException.class);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void managerCannotViewManagerAccount() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
		User manager = user(TARGET_USER_ID, RoleName.ROLE_MANAGER, UserStatus.ACTIVE);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(Optional.of(manager));

		assertThatThrownBy(() -> service.getById(TARGET_USER_ID))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void managerCannotLockOwnAccount() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		User current = user(CURRENT_USER_ID, RoleName.ROLE_USER, UserStatus.ACTIVE);
		when(userRepository.findByIdWithRoles(CURRENT_USER_ID)).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> service.block(CURRENT_USER_ID, request("Policy violation")))
				.isInstanceOf(ForbiddenException.class);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void adminCanLockManagerAndReasonIsStored() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		User manager = user(TARGET_USER_ID, RoleName.ROLE_MANAGER, UserStatus.ACTIVE);
		UserManagementResponse response = response(TARGET_USER_ID, UserStatus.BLOCKED);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(Optional.of(manager));
		when(userRepository.save(any(User.class))).thenReturn(manager);
		when(userManagementMapper.toResponse(manager)).thenReturn(response);

		UserManagementResponse result = service.block(TARGET_USER_ID, request("  Vi phạm quy định  "));

		assertThat(result).isEqualTo(response);
		assertThat(manager.getStatus()).isEqualTo(UserStatus.BLOCKED);
		assertThat(manager.getBlockReason()).isEqualTo("Vi phạm quy định");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void managerCanUnlockRegularBlockedUser() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		User blockedUser = user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.BLOCKED);
		blockedUser.setBlockReason("Đã xử lý vi phạm");
		blockedUser.setFailedLoginAttempts(4);
		UserManagementResponse response = response(TARGET_USER_ID, UserStatus.ACTIVE);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(Optional.of(blockedUser));
		when(userRepository.save(any(User.class))).thenReturn(blockedUser);
		when(userManagementMapper.toResponse(blockedUser)).thenReturn(response);

		UserManagementResponse result = service.unblock(TARGET_USER_ID);

		assertThat(result).isEqualTo(response);
		assertThat(blockedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(blockedUser.getBlockReason()).isNull();
		assertThat(blockedUser.getFailedLoginAttempts()).isZero();
		verify(userRepository).save(blockedUser);
	}

	@Test
	void searchExcludesAdminAndManagerFromManagerResults() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
		when(userRepository.searchByKeywordAndStatusAndRole(
				eq("buyer"), eq(UserStatus.ACTIVE), eq(RoleName.ROLE_USER),
				eq(true), eq(RoleName.ROLE_ADMIN), eq(RoleName.ROLE_MANAGER), any()))
				.thenReturn(org.springframework.data.domain.Page.empty());

		service.search(" buyer ", RoleName.ROLE_USER, UserStatus.ACTIVE,
				org.springframework.data.domain.PageRequest.of(0, 20));

		verify(userRepository).searchByKeywordAndStatusAndRole(
				"buyer", UserStatus.ACTIVE, RoleName.ROLE_USER, true,
				RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER,
				org.springframework.data.domain.PageRequest.of(0, 20));
	}

	@Test
	void searchFetchesRolesForThePaginatedUsers() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		User pageUser = user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.ACTIVE);
		User userWithRoles = user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.ACTIVE);
		UserManagementResponse response = response(TARGET_USER_ID, UserStatus.ACTIVE);
		PageRequest pageable = PageRequest.of(0, 20);
		when(userRepository.searchByKeywordAndStatusAndRole(
				null, null, null, false, RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER, pageable))
				.thenReturn(new PageImpl<>(java.util.List.of(pageUser), pageable, 1));
		when(userRepository.findAllWithRolesByIdIn(java.util.List.of(TARGET_USER_ID)))
				.thenReturn(java.util.List.of(userWithRoles));
		when(userManagementMapper.toResponse(userWithRoles)).thenReturn(response);

		var result = service.search(null, null, null, pageable);

		assertThat(result.content()).containsExactly(response);
		assertThat(result.totalElements()).isEqualTo(1);
	}

	private BlockUserRequest request(String reason) {
		BlockUserRequest request = new BlockUserRequest();
		request.setReason(reason);
		return request;
	}

	@Test
	void toggleCannotLockSelfEvenForAdmin() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		when(userRepository.findByIdWithRoles(CURRENT_USER_ID)).thenReturn(
				Optional.of(user(CURRENT_USER_ID, RoleName.ROLE_ADMIN, UserStatus.ACTIVE)));
		assertThatThrownBy(() -> service.toggleStatus(CURRENT_USER_ID,
				new ToggleUserStatusRequest(UserStatus.ACTIVE, "Vi phạm")))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void toggleDoesNotActivateUnverifiedAccount() {
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(
				Optional.of(user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.INACTIVE)));
		assertThatThrownBy(() -> service.toggleStatus(TARGET_USER_ID,
				new ToggleUserStatusRequest(UserStatus.INACTIVE, null)))
				.isInstanceOf(ConflictException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void toggleRequiresReasonAndRejectsStaleStatus() {
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(
				Optional.of(user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.ACTIVE)));
		assertThatThrownBy(() -> service.toggleStatus(TARGET_USER_ID,
				new ToggleUserStatusRequest(UserStatus.ACTIVE, "  ")))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.toggleStatus(TARGET_USER_ID,
				new ToggleUserStatusRequest(UserStatus.BLOCKED, null)))
				.isInstanceOf(ConflictException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void managerCannotEditPrivilegedAccount() {
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(
				Optional.of(user(TARGET_USER_ID, RoleName.ROLE_ADMIN, UserStatus.ACTIVE)));
		assertThatThrownBy(() -> service.update(TARGET_USER_ID, new UserUpdateRequest()))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void updateChangesOnlyNameAndPhone() {
		User target = user(TARGET_USER_ID, RoleName.ROLE_USER, UserStatus.ACTIVE);
		target.setEmail("original@example.com");
		var roles = target.getRoles();
		when(userRepository.findByIdWithRoles(TARGET_USER_ID)).thenReturn(Optional.of(target));
		when(userRepository.save(target)).thenReturn(target);
		UserUpdateRequest request = new UserUpdateRequest();
		request.setFullName("Nguyễn Văn An");
		request.setPhoneNumber("0901234567");
		service.update(TARGET_USER_ID, request);
		assertThat(target.getFullName()).isEqualTo("Nguyễn Văn An");
		assertThat(target.getPhoneNumber()).isEqualTo("0901234567");
		assertThat(target.getEmail()).isEqualTo("original@example.com");
		assertThat(target.getRoles()).isEqualTo(roles);
		assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	private User user(Long id, RoleName roleName, UserStatus status) {
		Role role = new Role();
		role.setName(roleName);
		User user = new User();
		user.setId(id);
		user.setRoles(Set.of(role));
		user.setStatus(status);
		return user;
	}

	private UserManagementResponse response(Long id, UserStatus status) {
		return new UserManagementResponse(id, "Test User", "test@example.com", null,
				java.util.List.of(RoleName.ROLE_USER), status, status.getDisplayName(),
				true, null, null, null);
	}
}
