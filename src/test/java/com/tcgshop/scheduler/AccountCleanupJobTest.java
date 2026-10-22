package com.tcgshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.repository.AccountCleanupRepository;
import com.tcgshop.repository.OtpTokenCleanupRepository;
import com.tcgshop.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountCleanupJobTest {
	@Mock
	private OtpTokenCleanupRepository otpTokenRepository;
	@Mock
	private AccountCleanupRepository accountCleanupRepository;
	@Mock
	private UserRepository userRepository;

	private AccountCleanupJob job;
	private LocalDateTime cutoff;

	@BeforeEach
	void setUp() {
		job = new AccountCleanupJob(otpTokenRepository, accountCleanupRepository, userRepository);
		cutoff = LocalDateTime.of(2026, 1, 2, 3, 0);
	}

	@Test
	void selectsOnlyOldInactiveUnverifiedUsersWithoutRelatedData() {
		User eligible = user(UserStatus.INACTIVE, false, cutoff.minusMinutes(1));
		eligible.setId(1L);
		User active = user(UserStatus.ACTIVE, true, cutoff.minusDays(2));
		active.setId(2L);
		User recent = user(UserStatus.INACTIVE, false, cutoff);
		recent.setId(3L);
		when(accountCleanupRepository.hasNoRelatedData(1L)).thenReturn(true);

		List<User> selected = job.selectUsersWithoutRelatedData(List.of(eligible, active, recent), cutoff);

		assertEquals(List.of(eligible), selected);
		verify(accountCleanupRepository).hasNoRelatedData(1L);
		verify(accountCleanupRepository, never()).hasNoRelatedData(2L);
		verify(accountCleanupRepository, never()).hasNoRelatedData(3L);
	}

	@Test
	void doesNotSelectUserWithRelatedData() {
		User user = user(UserStatus.INACTIVE, false, cutoff.minusDays(1));
		user.setId(8L);
		when(accountCleanupRepository.hasNoRelatedData(8L)).thenReturn(false);

		assertTrue(job.selectUsersWithoutRelatedData(List.of(user), cutoff).isEmpty());
	}

	@Test
	void otpRemovalMatchesUsedOrExpiredMoreThanRetentionWindow() {
		OtpToken used = new OtpToken();
		used.setUsed(true);
		OtpToken oldExpired = new OtpToken();
		oldExpired.setExpiresAt(cutoff);
		OtpToken recentlyExpired = new OtpToken();
		recentlyExpired.setExpiresAt(cutoff.plusSeconds(1));

		assertTrue(AccountCleanupJob.isRemovableOtpToken(used, cutoff));
		assertTrue(AccountCleanupJob.isRemovableOtpToken(oldExpired, cutoff));
		assertFalse(AccountCleanupJob.isRemovableOtpToken(recentlyExpired, cutoff));
	}

	private static User user(UserStatus status, boolean verified, LocalDateTime createdAt) {
		User user = new User();
		user.setStatus(status);
		user.setEmailVerified(verified);
		user.setCreatedAt(createdAt);
		return user;
	}
}
