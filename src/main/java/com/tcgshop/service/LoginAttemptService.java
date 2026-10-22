package com.tcgshop.service;

public interface LoginAttemptService {
	void recordFailure(String email);

	void recordSuccess(Long userId);
}
