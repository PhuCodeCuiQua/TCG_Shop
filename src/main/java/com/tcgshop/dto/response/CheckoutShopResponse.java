package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutShopResponse(
		Long shopId,
		String shopName,
		List<CheckoutLineResponse> lines,
		BigDecimal subtotal,
		CheckoutCouponResponse shopCoupon,
		BigDecimal platformCouponShare,
		BigDecimal discountAmount,
		Long carrierId,
		String carrierName,
		BigDecimal shippingFee,
		BigDecimal totalAmount,
		List<String> issues) {
	public CheckoutShopResponse {
		lines = lines == null ? List.of() : List.copyOf(lines);
		issues = issues == null ? List.of() : List.copyOf(issues);
	}
}
