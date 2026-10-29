package com.tcgshop.service;

public interface ReviewEligibilityService {
	boolean canReview(Long userId, Long productId);

	String getIneligibilityMessage(Long userId, Long productId);

	Long findEligibleOrderId(Long userId, Long productId);
}
