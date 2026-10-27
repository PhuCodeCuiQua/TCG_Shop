package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.ReviewSubmitRequest;
import com.tcgshop.dto.request.ReviewMediaRequest;
import com.tcgshop.dto.request.VendorReviewReplyRequest;
import com.tcgshop.dto.response.ReviewEligibilityResponse;
import com.tcgshop.dto.response.ReviewSubmitResponse;
import com.tcgshop.dto.response.ReviewReplyResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ReviewEligibilityService;
import com.tcgshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewRestController {
	private final ReviewEligibilityService eligibilityService;
	private final ReviewService reviewService;
	private final CurrentUser currentUser;

	@GetMapping("/eligibility/{productId}")
	public ResponseEntity<ReviewEligibilityResponse> eligibility(@PathVariable Long productId) {
		Long userId = requireUser();
		boolean eligible = eligibilityService.canReview(userId, productId);
		String message = eligible ? "Bạn đủ điều kiện đánh giá sản phẩm này."
				: eligibilityService.getIneligibilityMessage(userId, productId);
		return ResponseEntity.ok(new ReviewEligibilityResponse(eligible, message));
	}

	@PostMapping("/{productId}")
	public ResponseEntity<ReviewSubmitResponse> submit(@PathVariable Long productId,
			@Valid @RequestBody ReviewSubmitRequest request) {
		Long userId = requireUser();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(reviewService.submit(userId, productId, request));
	}

	@PostMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ReviewSubmitResponse> submitWithMedia(@PathVariable Long productId,
			@Valid @ModelAttribute ReviewMediaRequest request) {
		Long userId = requireUser();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(reviewService.submitWithMedia(userId, productId, request));
	}

	@PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ReviewSubmitResponse> update(@PathVariable Long reviewId,
			@Valid @ModelAttribute ReviewMediaRequest request) {
		return ResponseEntity.ok(reviewService.update(requireUser(), reviewId, request));
	}

	@DeleteMapping("/{reviewId}")
	public ResponseEntity<ReviewSubmitResponse> delete(@PathVariable Long reviewId) {
		return ResponseEntity.ok(reviewService.delete(requireUser(), reviewId));
	}

	@PostMapping("/{reviewId}/reply")
	public ResponseEntity<ReviewReplyResponse> reply(
			@PathVariable Long reviewId,
			@Valid @RequestBody VendorReviewReplyRequest request) {
		return ResponseEntity.ok(reviewService.replyToReview(reviewId, request));
	}

	private Long requireUser() {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER)) {
			throw new ForbiddenException("Chức năng đánh giá chỉ dành cho người mua");
		}
		return currentUser.getId();
	}
}
