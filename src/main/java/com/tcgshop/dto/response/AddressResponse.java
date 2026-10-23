package com.tcgshop.dto.response;

import java.time.LocalDateTime;

public record AddressResponse(
		Long id,
		String receiverName,
		String phoneNumber,
		String province,
		String district,
		String ward,
		String detailAddress,
		String fullAddress,
		boolean isDefault,
		LocalDateTime createdAt) {
}
