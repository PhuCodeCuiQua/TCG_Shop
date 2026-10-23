package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ShopStatus;

public record VendorShopManageResponse(
		Long id,
		String name,
		String logoUrl,
		String bannerUrl,
		String description,
		String phoneNumber,
		String pickupAddress,
		ShopStatus status) {
}
