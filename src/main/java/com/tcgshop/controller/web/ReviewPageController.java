package com.tcgshop.controller.web;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ReviewResponse;
import com.tcgshop.dto.request.ReviewMediaRequest;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ReviewEligibilityService;
import com.tcgshop.service.ReviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Public fragment endpoint for product reviews. */
@Controller
@RequiredArgsConstructor
public class ReviewPageController {
	private static final int PAGE_SIZE = 5;
	private static final Sort REVIEW_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	private final ReviewService reviewService;
	private final ReviewEligibilityService eligibilityService;
	private final CurrentUser currentUser;

	@GetMapping("/products/{productId}/reviews")
	public String reviews(@PathVariable Long productId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) Integer rating,
			Model model) {
		int safePage = Math.max(0, page);
		var product = reviewService.getProductSummary(productId);
		PageResponse<ReviewResponse> response = rating == null
				? reviewService.getReviewsByProduct(productId, PageRequest.of(safePage, PAGE_SIZE, REVIEW_SORT))
				: reviewService.getReviewsByProduct(productId, rating,
						PageRequest.of(safePage, PAGE_SIZE, REVIEW_SORT));
		PageImpl<ReviewResponse> reviews = new PageImpl<>(
				response.content(), PageRequest.of(response.page(), response.size(), REVIEW_SORT),
				response.totalElements());
		boolean user = currentUser.isAuthenticated() && currentUser.hasRole(RoleName.ROLE_USER);
		boolean authenticated = currentUser.isAuthenticated();
		boolean canReview = user && eligibilityService.canReview(currentUser.getId(), productId);
		String eligibilityMessage = user
				? (canReview ? "Bạn đủ điều kiện đánh giá sản phẩm này."
						: eligibilityService.getIneligibilityMessage(currentUser.getId(), productId))
				: "Đăng nhập bằng tài khoản người mua để kiểm tra điều kiện đánh giá.";

		model.addAttribute("product", product);
		model.addAttribute("reviews", reviews);
		model.addAttribute("selectedRating", rating);
		model.addAttribute("ratingFilterOptions", List.of(5, 4, 3, 2, 1));
		model.addAttribute("canReplyToProduct", reviewService.canCurrentVendorReplyToProduct(productId));
		model.addAttribute("canReview", canReview);
		model.addAttribute("isReviewUser", user);
		model.addAttribute("isReviewAuthenticated", authenticated);
		model.addAttribute("eligibilityMessage", eligibilityMessage);
		model.addAttribute("reviewForm", new ReviewMediaRequest());
		model.addAttribute("currentUserId", authenticated ? currentUser.getId() : null);
		model.addAttribute("ratingStars", List.of(1, 2, 3, 4, 5));
		return "fragments/reviews :: reviewSection";
	}
}
