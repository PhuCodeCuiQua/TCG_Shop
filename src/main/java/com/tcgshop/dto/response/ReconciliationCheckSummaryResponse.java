package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ReconciliationSeverity;

public record ReconciliationCheckSummaryResponse(String code, String title,
		ReconciliationSeverity severity, long violationCount) {
}
