package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.RoleName;
import java.util.Set;

public record AuthTokensResponse(String accessToken, String refreshToken,
		Set<RoleName> roles, boolean mustChangePassword) {
}
