package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ReconciliationSeverity;

public record ReconciliationFindingResponse(String checkCode, ReconciliationSeverity severity,
		String entityType, Long entityId, String entityCode, String detail) {
}
