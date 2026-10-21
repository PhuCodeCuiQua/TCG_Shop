package com.tcgshop.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CustomUserDetailsTest {

	@Test
	void activeUserIsEnabledAndNonLocked() {
		CustomUserDetails userDetails = createUserDetails(UserStatus.ACTIVE, null);

		assertTrue(userDetails.isEnabled());
		assertTrue(userDetails.isAccountNonLocked());
	}

	@Test
	void inactiveUserIsDisabled() {
		CustomUserDetails userDetails = createUserDetails(UserStatus.INACTIVE, null);

		assertFalse(userDetails.isEnabled());
		assertTrue(userDetails.isAccountNonLocked());
	}

	@Test
	void blockedUserIsLocked() {
		CustomUserDetails userDetails = createUserDetails(UserStatus.BLOCKED, null);

		assertFalse(userDetails.isEnabled());
		assertFalse(userDetails.isAccountNonLocked());
	}

	@Test
	void userWithFutureLockedUntilIsLocked() {
		CustomUserDetails userDetails = createUserDetails(
				UserStatus.ACTIVE, LocalDateTime.now().plusMinutes(1));

		assertFalse(userDetails.isAccountNonLocked());
	}

	@Test
	void userWithExpiredLockedUntilIsNotLocked() {
		CustomUserDetails userDetails = createUserDetails(
				UserStatus.ACTIVE, LocalDateTime.now().minusMinutes(1));

		assertTrue(userDetails.isAccountNonLocked());
	}

	@Test
	void copiesIdentityAndAuthoritiesFromEntity() {
		User entity = createUser(UserStatus.ACTIVE, null);
		CustomUserDetails userDetails = new CustomUserDetails(entity);

		entity.setEmail("changed@example.com");
		entity.setStatus(UserStatus.BLOCKED);
		entity.getRoles().clear();

		assertEquals(42L, userDetails.getId());
		assertEquals("buyer@example.com", userDetails.getEmail());
		assertEquals(UserStatus.ACTIVE, userDetails.getStatus());
		assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_USER")), Set.copyOf(userDetails.getAuthorities()));
	}

	private CustomUserDetails createUserDetails(UserStatus status, LocalDateTime lockedUntil) {
		return new CustomUserDetails(createUser(status, lockedUntil));
	}

	private User createUser(UserStatus status, LocalDateTime lockedUntil) {
		User user = new User();
		user.setId(42L);
		user.setEmail("buyer@example.com");
		user.setPassword("$2a$encoded");
		user.setStatus(status);
		user.setLockedUntil(lockedUntil);

		Role role = new Role();
		role.setName(RoleName.ROLE_USER);
		user.setRoles(new HashSet<>(Set.of(role)));

		return user;
	}
}
