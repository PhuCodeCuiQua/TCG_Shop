package com.tcgshop.service;

import com.tcgshop.dto.request.VendorCouponRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.VendorCouponResponse;
import org.springframework.data.domain.Pageable;

public interface VendorCouponService {
	PageResponse<VendorCouponResponse> getCurrentShopCoupons(Pageable pageable);

	VendorCouponResponse getCurrentShopCoupon(Long couponId);

	VendorCouponResponse saveCurrentShopCoupon(Long couponId, VendorCouponRequest request);

	VendorCouponResponse toggleCurrentShopCoupon(Long couponId);

	VendorCouponResponse deactivateCurrentShopCoupon(Long couponId);
}
