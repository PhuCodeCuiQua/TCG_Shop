package com.tcgshop.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReconciliationReportResponse(LocalDateTime generatedAt, long ordersChecked,
		long errorCount, long warningCount, List<ReconciliationCheckSummaryResponse> checks,
		List<ReconciliationFindingResponse> findings) {
	public ReconciliationReportResponse {
		checks = List.copyOf(checks == null ? List.of() : checks);
		findings = List.copyOf(findings == null ? List.of() : findings);
	}
}
