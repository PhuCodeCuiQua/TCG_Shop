package com.tcgshop.scheduler;

import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.repository.AccountCleanupRepository;
import com.tcgshop.repository.OtpTokenCleanupRepository;
import com.tcgshop.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AccountCleanupJob {
	private static final int RETENTION_HOURS = 24;

	private final OtpTokenCleanupRepository otpTokenRepository;
	private final AccountCleanupRepository accountCleanupRepository;
	private final UserRepository userRepository;

	public AccountCleanupJob(OtpTokenCleanupRepository otpTokenRepository,
			AccountCleanupRepository accountCleanupRepository, UserRepository userRepository) {
		this.otpTokenRepository = otpTokenRepository;
		this.accountCleanupRepository = accountCleanupRepository;
		this.userRepository = userRepository;
	}

	/** Chạy hằng ngày lúc 03:00; mọi thao tác dọn trong một transaction. */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanup() {
		LocalDateTime cutoff = LocalDateTime.now().minusHours(RETENTION_HOURS);
		int deletedOtpTokens = otpTokenRepository.deleteUsedOrExpiredBefore(cutoff);

		List<User> candidates = accountCleanupRepository
				.findByStatusAndEmailVerifiedFalseAndCreatedAtBefore(UserStatus.INACTIVE, cutoff);
		int deletedUsers = 0;
		for (User user : selectUsersWithoutRelatedData(candidates, cutoff)) {
			userRepository.delete(user);
			deletedUsers++;
		}
		log.info("Account cleanup completed: deleted {} OTP tokens and {} inactive users",
				deletedOtpTokens, deletedUsers);
	}

	/** Chỉ xóa user đủ điều kiện tuổi/trạng thái và không còn bản ghi nào tham chiếu tới user đó. */
	List<User> selectUsersWithoutRelatedData(List<User> candidates, LocalDateTime cutoff) {
		return candidates.stream()
				.filter(user -> isStaleInactiveUnverified(user, cutoff))
				.filter(user -> accountCleanupRepository.hasNoRelatedData(user.getId()))
				.toList();
	}

	static boolean isStaleInactiveUnverified(User user, LocalDateTime cutoff) {
		return user.getStatus() == UserStatus.INACTIVE
				&& !user.isEmailVerified()
				&& user.getCreatedAt() != null
				&& user.getCreatedAt().isBefore(cutoff);
	}

	static boolean isRemovableOtpToken(OtpToken token, LocalDateTime cutoff) {
		return token.isUsed() || (token.getExpiresAt() != null && !token.getExpiresAt().isAfter(cutoff));
	}
}
