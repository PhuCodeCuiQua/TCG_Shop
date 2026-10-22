package com.tcgshop.service;

import com.tcgshop.dto.response.CouponApplyResponse;
import java.math.BigDecimal;

public interface CouponService {
	CouponApplyResponse validateAndCalculate(String code, Long userId, Long shopId, BigDecimal subtotal);

	void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount);
}
