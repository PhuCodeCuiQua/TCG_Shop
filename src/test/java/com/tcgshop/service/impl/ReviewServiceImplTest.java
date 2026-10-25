package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Review;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.repository.PublicReviewRepository;
import com.tcgshop.repository.ReviewAggregateRepository;
import com.tcgshop.repository.ReviewEligibilityRepository;
import com.tcgshop.repository.ReviewMediaListingRepository;
import com.tcgshop.repository.ReviewProductRepository;
import com.tcgshop.repository.ReviewRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.ReviewEligibilityService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
	private static final Long CURRENT_USER_ID = 12L;
	private static final Long OTHER_USER_ID = 98L;
	private static final Long REVIEW_ID = 140L;

	@Mock private PublicReviewRepository publicReviewRepository;
	@Mock private ReviewRepository reviewRepository;
	@Mock private ReviewAggregateRepository aggregateRepository;
	@Mock private ReviewEligibilityRepository eligibilityRepository;
	@Mock private ReviewProductRepository productRepository;
	@Mock private UserRepository userRepository;
	@Mock private ReviewMediaListingRepository mediaListingRepository;
	@Mock private ReviewEligibilityService eligibilityService;
	@Mock private ReviewMapper reviewMapper;
	@Mock private CurrentUser currentUser;
	@Mock private CloudinaryService cloudinaryService;
	@InjectMocks private ReviewServiceImpl service;

	@Test
	void rejectsDeletingAnotherUsersReview() {
		User owner = new User();
		owner.setId(OTHER_USER_ID);
		Review review = new Review();
		review.setUser(owner);
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(CURRENT_USER_ID);
		when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

		assertThatThrownBy(() -> service.delete(CURRENT_USER_ID, REVIEW_ID))
				.isInstanceOf(ForbiddenException.class);
		verify(reviewRepository, never()).saveAndFlush(review);
	}
}
