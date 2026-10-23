package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ShopStatus;

public record ShopRegistrationResponse(
		Long id,
		String name,
		String description,
		String logoUrl,
		String phoneNumber,
		String pickupAddress,
		ShopStatus status,
		String rejectReason) {
}
