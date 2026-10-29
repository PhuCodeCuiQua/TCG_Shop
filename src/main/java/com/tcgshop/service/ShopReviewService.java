package com.tcgshop.service;

import com.tcgshop.dto.request.ShopLockRequest;
import com.tcgshop.dto.request.ShopProfileUpdateRequest;
import com.tcgshop.dto.request.ShopRejectRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShopReviewResponse;
import com.tcgshop.entity.enums.ShopStatus;
import org.springframework.data.domain.Pageable;

public interface ShopReviewService {

	PageResponse<ShopReviewResponse> search(String keyword, ShopStatus status, Pageable pageable);

	ShopReviewResponse getById(Long shopId);

	ShopReviewResponse updateProfile(Long shopId, ShopProfileUpdateRequest request);

	ShopReviewResponse approve(Long shopId);

	ShopReviewResponse reject(Long shopId, ShopRejectRequest request);

	ShopReviewResponse suspend(Long shopId, ShopLockRequest request);

	ShopReviewResponse reactivate(Long shopId);
}
