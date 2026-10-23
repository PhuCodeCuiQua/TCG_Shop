package com.tcgshop.service;

import com.tcgshop.dto.request.PlatformCouponRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PlatformCouponResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import org.springframework.data.domain.Pageable;

public interface PlatformCouponAdminService {

	PageResponse<PlatformCouponResponse> search(ActiveStatus status, Pageable pageable);

	PlatformCouponResponse getById(Long couponId);

	PlatformCouponResponse save(Long couponId, PlatformCouponRequest request);

	PlatformCouponResponse setStatus(Long couponId, ActiveStatus status);
}
