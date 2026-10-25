package com.tcgshop.service.impl;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.repository.ReviewEligibilityRepository;
import com.tcgshop.repository.ReviewProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ReviewEligibilityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewEligibilityServiceImpl implements ReviewEligibilityService {
	private static final String PURCHASE_REQUIRED = "Bạn cần mua sản phẩm này và đơn hàng phải giao thành công để đánh giá.";
	private static final String ALREADY_REVIEWED = "Bạn đã đánh giá sản phẩm trong tất cả đơn hàng đã giao.";

	private final ReviewEligibilityRepository eligibilityRepository;
	private final ReviewProductRepository productRepository;
	private final CurrentUser currentUser;

	@Override
	public boolean canReview(Long userId, Long productId) {
		validateActor(userId);
		return productRepository.findPublicProduct(productId).isPresent()
				&& !eligibilityRepository.findEligibleOrderIds(userId, productId, PageRequest.of(0, 1)).isEmpty();
	}

	@Override
	public String getIneligibilityMessage(Long userId, Long productId) {
		validateActor(userId);
		if (productRepository.findPublicProduct(productId).isEmpty()) {
			return "Sản phẩm hiện không còn được mở bán công khai.";
		}
		if (eligibilityRepository.countDeliveredPurchases(userId, productId) == 0) {
			return PURCHASE_REQUIRED;
		}
		return eligibilityRepository.findEligibleOrderIds(userId, productId, PageRequest.of(0, 1)).isEmpty()
				? ALREADY_REVIEWED : "Bạn đủ điều kiện đánh giá sản phẩm này.";
	}

	@Override
	public Long findEligibleOrderId(Long userId, Long productId) {
		validateActor(userId);
		List<Long> orderIds = eligibilityRepository.findEligibleOrderIds(
				userId, productId, PageRequest.of(0, 1));
		if (orderIds.isEmpty()) {
			throw new BadRequestException(getIneligibilityMessage(userId, productId));
		}
		return orderIds.get(0);
	}

	private void validateActor(Long userId) {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER) || userId == null || !userId.equals(currentUser.getId())) {
			throw new ForbiddenException("Chỉ người mua đang đăng nhập mới được đánh giá sản phẩm");
		}
	}
}
