package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record CouponApplyResponse(
		boolean valid,
		Long couponId,
		BigDecimal discountAmount,
		String message) {
}
