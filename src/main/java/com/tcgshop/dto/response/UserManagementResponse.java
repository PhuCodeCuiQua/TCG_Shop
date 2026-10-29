package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public record UserManagementResponse(
		Long id,
		String fullName,
		String email,
		String phoneNumber,
		List<RoleName> roles,
		UserStatus status,
		String statusDisplayName,
		boolean emailVerified,
		String blockReason,
		LocalDateTime lockedUntil,
		LocalDateTime createdAt) {
}
