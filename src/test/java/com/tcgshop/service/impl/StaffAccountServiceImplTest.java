package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.StaffAccountRequest;
import com.tcgshop.dto.response.StaffAccountResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.StaffAccountMapper;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.EmailService;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceImplTest {
	private static final String EMAIL = "new.staff@example.com";
	private static final Pattern TEMPORARY_PASSWORD_PATTERN =
			Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d]{12}$");

	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private StaffAccountMapper staffAccountMapper;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private EmailService emailService;
	@Mock
	private CurrentUser currentUser;
	@InjectMocks
	private StaffAccountServiceImpl service;

	@Test
	void adminCreatesManagerWithSecureTemporaryPasswordAndFirstLoginChangeRequired() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(role(RoleName.ROLE_MANAGER)));
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		StaffAccountResponse expected = response(RoleName.ROLE_MANAGER);
		when(staffAccountMapper.toResponse(any(User.class), eq(0L))).thenReturn(expected);

		StaffAccountResponse result = service.createManager(request());

		assertThat(result).isEqualTo(expected);
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
		assertThat(savedUser.getFullName()).isEqualTo("Nguyễn Văn A");
		assertThat(savedUser.getPhoneNumber()).isEqualTo("0912345678");
		assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(savedUser.isEmailVerified()).isTrue();
		assertThat(savedUser.isMustChangePassword()).isTrue();
		assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
		assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_MANAGER);

		ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
		verify(passwordEncoder).encode(passwordCaptor.capture());
		assertThat(passwordCaptor.getValue()).matches(TEMPORARY_PASSWORD_PATTERN);
		verify(emailService).sendTemporaryPasswordEmail(
				EMAIL, "Nguyễn Văn A", passwordCaptor.getValue(), RoleName.ROLE_MANAGER.getDisplayName());
	}

	@Test
	void managerCreatesShipperWithOnlyShipperRole() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(roleRepository.findByName(RoleName.ROLE_SHIPPER)).thenReturn(Optional.of(role(RoleName.ROLE_SHIPPER)));
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(staffAccountMapper.toResponse(any(User.class), eq(0L))).thenReturn(response(RoleName.ROLE_SHIPPER));

		service.createShipper(request());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getRoles())
				.extracting(Role::getName).containsExactly(RoleName.ROLE_SHIPPER);
		verify(emailService).sendTemporaryPasswordEmail(
				eq(EMAIL), eq("Nguyễn Văn A"), any(), eq(RoleName.ROLE_SHIPPER.getDisplayName()));
	}

	@Test
	void managerCannotCreateManager() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> service.createManager(request()))
				.isInstanceOf(ForbiddenException.class);

		verify(userRepository, never()).existsByEmail(any());
	}

	@Test
	void userWithoutManagerOrAdminRoleCannotListShippers() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> service.searchShippers(null, null,
				org.springframework.data.domain.PageRequest.of(0, 20)))
				.isInstanceOf(ForbiddenException.class);

		verify(userRepository, never()).searchByKeywordAndStatusAndRole(
				any(), any(), any(), any(Boolean.class), any(), any(), any());
	}

	@Test
	void duplicateEmailIsRejectedBeforeRoleLookupOrSave() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

		assertThatThrownBy(() -> service.createManager(request()))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Email này đã được sử dụng");

		verify(roleRepository, never()).findByName(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void missingRequiredRoleIsReported() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createManager(request()))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	void emailFailurePropagatesSoTransactionalAccountCreationRollsBack() throws NoSuchMethodException {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(role(RoleName.ROLE_MANAGER)));
		when(passwordEncoder.encode(any())).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		org.mockito.Mockito.doThrow(new BusinessException("Không thể gửi email"))
				.when(emailService).sendTemporaryPasswordEmail(eq(EMAIL), eq("Nguyễn Văn A"),
						any(), eq(RoleName.ROLE_MANAGER.getDisplayName()));

		assertThatThrownBy(() -> service.createManager(request()))
				.isInstanceOf(BusinessException.class);

		assertThat(StaffAccountServiceImpl.class
				.getMethod("createManager", StaffAccountRequest.class)
				.isAnnotationPresent(Transactional.class)).isTrue();
		verify(staffAccountMapper, never()).toResponse(any(User.class), any(Long.class));
	}

	@Test
	void listingCountsOrdersInShippingStatus() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		User shipper = new User();
		shipper.setId(15L);
		shipper.setRoles(Set.of(role(RoleName.ROLE_SHIPPER)));
		var page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(shipper), pageable, 1);
		when(userRepository.searchByKeywordAndStatusAndRole(
				"ship", UserStatus.ACTIVE, RoleName.ROLE_SHIPPER,
				false, RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER, pageable))
				.thenReturn(page);
		when(orderRepository.countByShipper_IdAndStatus(15L, OrderStatus.SHIPPING)).thenReturn(4L);
		StaffAccountResponse expected = new StaffAccountResponse(
				15L, "Nguyễn Văn A", EMAIL, "0912345678", UserStatus.ACTIVE,
				UserStatus.ACTIVE.getDisplayName(), 4L);
		when(staffAccountMapper.toResponse(shipper, 4L)).thenReturn(expected);

		var result = service.searchShippers(" ship ", UserStatus.ACTIVE, pageable);

		assertThat(result.content()).containsExactly(expected);
		verify(orderRepository).countByShipper_IdAndStatus(15L, OrderStatus.SHIPPING);
	}

	@Test
	void nullRequestIsRejected() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> service.createManager(null))
				.isInstanceOf(BadRequestException.class);
	}

	private StaffAccountRequest request() {
		StaffAccountRequest request = new StaffAccountRequest();
		request.setFullName(" Nguyễn Văn A ");
		request.setEmail(EMAIL);
		request.setPhoneNumber("0912345678");
		return request;
	}

	private Role role(RoleName name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}

	private StaffAccountResponse response(RoleName roleName) {
		return new StaffAccountResponse(
				21L, "Nguyễn Văn A", EMAIL, "0912345678",
				UserStatus.ACTIVE, UserStatus.ACTIVE.getDisplayName(), 0);
	}
}
