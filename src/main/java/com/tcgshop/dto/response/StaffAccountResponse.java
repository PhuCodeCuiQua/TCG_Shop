package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.UserStatus;

public record StaffAccountResponse(
		Long id,
		String fullName,
		String email,
		String phoneNumber,
		UserStatus status,
		String statusDisplayName,
		long activeDeliveryCount) {
}
