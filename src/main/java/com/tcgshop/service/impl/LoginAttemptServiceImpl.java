package com.tcgshop.service.impl;

import com.tcgshop.config.LoginProperties;
import com.tcgshop.entity.User;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.LoginAttemptService;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {
	private final UserRepository userRepository;
	private final LoginProperties loginProperties;

	public LoginAttemptServiceImpl(UserRepository userRepository, LoginProperties loginProperties) {
		this.userRepository = userRepository;
		this.loginProperties = loginProperties;
	}

	/**
	 * Bean riêng và REQUIRES_NEW đảm bảo cập nhật bộ đếm không bị mất khi AuthService ném lỗi xác thực.
	 * Nếu tự gọi nội bộ trong cùng bean, Spring không áp dụng proxy transaction (self-invocation).
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailure(String email) {
		userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
			int attempts = user.getFailedLoginAttempts() + 1;
			if (attempts >= loginProperties.maxFailedAttempts()) {
				user.setFailedLoginAttempts(0);
				user.setLockedUntil(LocalDateTime.now().plusMinutes(loginProperties.lockMinutes()));
			} else {
				user.setFailedLoginAttempts(attempts);
			}
			userRepository.save(user);
		});
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordSuccess(Long userId) {
		userRepository.findById(userId).ifPresent(user -> {
			user.setFailedLoginAttempts(0);
			user.setLockedUntil(null);
			userRepository.save(user);
		});
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
