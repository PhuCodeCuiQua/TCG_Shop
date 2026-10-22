package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.config.LoginProperties;
import com.tcgshop.entity.User;
import com.tcgshop.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {
	@Mock
	private UserRepository userRepository;

	private LoginAttemptServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LoginAttemptServiceImpl(userRepository, new LoginProperties(5, 15));
	}

	@Test
	void fifthFailureLocksUserForConfiguredMinutesAndResetsCounter() {
		User user = new User();
		user.setFailedLoginAttempts(4);
		when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

		service.recordFailure(" Buyer@Example.com ");

		assertEquals(0, user.getFailedLoginAttempts());
		assertNotNull(user.getLockedUntil());
		assertTrue(user.getLockedUntil().isAfter(java.time.LocalDateTime.now().plusMinutes(14)));
		assertFalse(user.getLockedUntil().isAfter(java.time.LocalDateTime.now().plusMinutes(15)));
		verify(userRepository).save(user);
	}

	@Test
	void successResetsFailureCounterAndLock() {
		User user = new User();
		user.setFailedLoginAttempts(3);
		user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(5));
		when(userRepository.findById(12L)).thenReturn(Optional.of(user));

		service.recordSuccess(12L);

		assertEquals(0, user.getFailedLoginAttempts());
		assertNull(user.getLockedUntil());
		verify(userRepository).save(user);
	}
}
