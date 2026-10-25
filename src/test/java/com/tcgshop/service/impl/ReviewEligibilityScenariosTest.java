package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.repository.ReviewEligibilityRepository;
import com.tcgshop.repository.ReviewProductRepository;
import com.tcgshop.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReviewEligibilityScenariosTest {
	private static final Long USER_ID = 31L;
	private static final Long PRODUCT_ID = 72L;

	@Mock private ReviewEligibilityRepository eligibilityRepository;
	@Mock private ReviewProductRepository productRepository;
	@Mock private CurrentUser currentUser;
	private ReviewEligibilityServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ReviewEligibilityServiceImpl(eligibilityRepository, productRepository, currentUser);
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(USER_ID);
		when(productRepository.findPublicProduct(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
	}

	@Test
	void cannotReviewProductThatUserHasNotPurchased() {
		givenNoEligibleDeliveredOrder();
		when(eligibilityRepository.countDeliveredPurchases(USER_ID, PRODUCT_ID)).thenReturn(0L);

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isFalse();
	}

	@Test
	void cannotReviewBeforeOrderIsDelivered() {
		// Truy vấn service chỉ trả đơn DELIVERED; đơn NEW/CONFIRMED không xuất hiện trong tập đủ điều kiện.
		givenNoEligibleDeliveredOrder();
		when(eligibilityRepository.countDeliveredPurchases(USER_ID, PRODUCT_ID)).thenReturn(0L);

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isFalse();
	}

	@Test
	void cannotReviewProductAlreadyReviewedInDeliveredOrder() {
		givenNoEligibleDeliveredOrder();
		when(eligibilityRepository.countDeliveredPurchases(USER_ID, PRODUCT_ID)).thenReturn(1L);

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isFalse();
	}

	@Test
	void canReviewProductWithUnreviewedDeliveredOrder() {
		when(eligibilityRepository.findEligibleOrderIds(USER_ID, PRODUCT_ID, PageRequest.of(0, 1)))
				.thenReturn(List.of(501L));

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isTrue();
	}

	private void givenNoEligibleDeliveredOrder() {
		when(eligibilityRepository.findEligibleOrderIds(USER_ID, PRODUCT_ID, PageRequest.of(0, 1)))
				.thenReturn(List.of());
	}
}
