package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.RoleName;
import java.time.LocalDateTime;
import java.util.Set;

public record ProfileResponse(
		String email,
		String fullName,
		String phoneNumber,
		String avatarUrl,
		Set<RoleName> roles,
		LocalDateTime createdAt) { }
