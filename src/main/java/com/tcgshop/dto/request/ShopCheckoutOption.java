package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShopCheckoutOption(
		@NotNull(message = "Vui lòng chọn shop") Long shopId,
		Long carrierId,
		@Size(max = 50, message = "Mã giảm giá không được vượt quá 50 ký tự") String couponCode) {
}
