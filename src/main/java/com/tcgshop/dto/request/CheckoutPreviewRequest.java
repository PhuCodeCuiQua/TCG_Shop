package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CheckoutPreviewRequest(
		Long addressId,
		List<@NotNull @Valid ShopCheckoutOption> shops,
		@Size(max = 50, message = "Mã giảm giá sàn không được vượt quá 50 ký tự") String platformCouponCode,
		PaymentMethod paymentMethod) {

	@AssertTrue(message = "Mỗi shop chỉ được chọn một lần")
	public boolean isShopIdsUnique() {
		if (shops == null) {
			return true;
		}
		Set<Long> seenShopIds = new HashSet<>();
		return shops.stream().filter(Objects::nonNull).map(ShopCheckoutOption::shopId)
				.filter(Objects::nonNull).allMatch(seenShopIds::add);
	}
}
