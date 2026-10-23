package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionSettingResponse(
		Long id,
		BigDecimal defaultRate,
		String updatedByName,
		LocalDateTime createdAt) {
}
