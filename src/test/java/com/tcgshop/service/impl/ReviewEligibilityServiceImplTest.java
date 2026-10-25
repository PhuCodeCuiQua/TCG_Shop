package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.Product;
import com.tcgshop.exception.ForbiddenException;
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
class ReviewEligibilityServiceImplTest {
	private static final Long USER_ID = 31L;
	private static final Long PRODUCT_ID = 72L;

	@Mock private ReviewEligibilityRepository eligibilityRepository;
	@Mock private ReviewProductRepository productRepository;
	@Mock private CurrentUser currentUser;

	private ReviewEligibilityServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ReviewEligibilityServiceImpl(eligibilityRepository, productRepository, currentUser);
		lenient().when(currentUser.isAuthenticated()).thenReturn(true);
		lenient().when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		lenient().when(currentUser.getId()).thenReturn(USER_ID);
	}

	@Test
	void canReviewWhenThereIsAnUnreviewedDeliveredPurchase() {
		when(productRepository.findPublicProduct(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
		when(eligibilityRepository.findEligibleOrderIds(USER_ID, PRODUCT_ID, PageRequest.of(0, 1)))
				.thenReturn(List.of(501L));

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isTrue();
	}

	@Test
	void cannotReviewWhenNoDeliveredPurchaseExists() {
		when(productRepository.findPublicProduct(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
		when(eligibilityRepository.findEligibleOrderIds(USER_ID, PRODUCT_ID, PageRequest.of(0, 1)))
				.thenReturn(List.of());
		when(eligibilityRepository.countDeliveredPurchases(USER_ID, PRODUCT_ID)).thenReturn(0L);

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isFalse();
		assertThat(service.getIneligibilityMessage(USER_ID, PRODUCT_ID))
				.contains("đơn hàng phải giao thành công");
	}

	@Test
	void cannotReviewWhenEveryDeliveredPurchaseAlreadyHasAReview() {
		when(productRepository.findPublicProduct(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
		when(eligibilityRepository.findEligibleOrderIds(USER_ID, PRODUCT_ID, PageRequest.of(0, 1)))
				.thenReturn(List.of());
		when(eligibilityRepository.countDeliveredPurchases(USER_ID, PRODUCT_ID)).thenReturn(1L);

		assertThat(service.canReview(USER_ID, PRODUCT_ID)).isFalse();
		assertThat(service.getIneligibilityMessage(USER_ID, PRODUCT_ID))
				.contains("đã đánh giá sản phẩm");
	}

	@Test
	void rejectsCheckingEligibilityForAnotherUser() {
		when(currentUser.getId()).thenReturn(USER_ID + 1);

		assertThatThrownBy(() -> service.canReview(USER_ID, PRODUCT_ID))
				.isInstanceOf(ForbiddenException.class);
	}
}
