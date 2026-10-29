package com.tcgshop.service;

import com.tcgshop.dto.request.ReviewSubmitRequest;
import com.tcgshop.dto.request.ReviewMediaRequest;
import com.tcgshop.dto.request.VendorReviewReplyRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ReviewResponse;
import com.tcgshop.dto.response.ReviewReplyResponse;
import com.tcgshop.dto.response.ReviewSubmitResponse;
import com.tcgshop.dto.response.ReviewProductResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
	PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);

	PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Integer rating, Pageable pageable);

	boolean canCurrentVendorReplyToProduct(Long productId);

	ReviewReplyResponse replyToReview(Long reviewId, VendorReviewReplyRequest request);

	ReviewProductResponse getProductSummary(Long productId);

	ReviewSubmitResponse submit(Long userId, Long productId, ReviewSubmitRequest request);

	ReviewSubmitResponse submitWithMedia(Long userId, Long productId, ReviewMediaRequest request);

	ReviewSubmitResponse update(Long userId, Long reviewId, ReviewMediaRequest request);

	ReviewSubmitResponse delete(Long userId, Long reviewId);
}
