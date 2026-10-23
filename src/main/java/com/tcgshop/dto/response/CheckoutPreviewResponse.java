package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
		AddressResponse address,
		PaymentMethod paymentMethod,
		List<ShippingCarrierResponse> availableCarriers,
		List<CheckoutShopResponse> shops,
		CheckoutCouponResponse platformCoupon,
		BigDecimal itemsSubtotal,
		BigDecimal totalDiscount,
		BigDecimal totalShippingFee,
		BigDecimal grandTotal,
		List<String> issues,
		boolean canPlaceOrder) {
	public CheckoutPreviewResponse {
		availableCarriers = availableCarriers == null ? List.of() : List.copyOf(availableCarriers);
		shops = shops == null ? List.of() : List.copyOf(shops);
		issues = issues == null ? List.of() : List.copyOf(issues);
	}
}
