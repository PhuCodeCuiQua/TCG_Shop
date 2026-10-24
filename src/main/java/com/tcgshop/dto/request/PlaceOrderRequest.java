package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
		Long addressId,
		@Valid List<@NotNull @Valid ShopCheckoutOption> shops,
		@Size(max = 50) String platformCouponCode,
		@NotNull PaymentMethod paymentMethod,
		@NotBlank @Pattern(regexp = "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String idempotencyToken,
		@NotNull @PositiveOrZero BigDecimal expectedGrandTotal,
		@Size(max = 500) String note) {

	public CheckoutPreviewRequest toPreviewRequest() {
		return new CheckoutPreviewRequest(addressId, shops, platformCouponCode, paymentMethod);
	}
}
