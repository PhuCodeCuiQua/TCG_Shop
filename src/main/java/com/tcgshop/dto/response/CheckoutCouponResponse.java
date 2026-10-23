package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record CheckoutCouponResponse(
		String code,
		boolean applied,
		Long couponId,
		BigDecimal discountAmount,
		String message) {
}
