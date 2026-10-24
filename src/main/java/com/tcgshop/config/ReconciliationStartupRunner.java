package com.tcgshop.config;

import com.tcgshop.dto.response.ReconciliationFindingResponse;
import com.tcgshop.dto.response.ReconciliationReportResponse;
import com.tcgshop.service.OrderReconciliationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.reconcile", name = "run-on-startup", havingValue = "true")
public class ReconciliationStartupRunner implements ApplicationRunner {
	private final OrderReconciliationService reconciliationService;

	@Override
	public void run(ApplicationArguments args) {
		try {
			ReconciliationReportResponse report = reconciliationService.reconcile();
			log.info("Order reconciliation completed: orders={}, errors={}, warnings={}",
					report.ordersChecked(), report.errorCount(), report.warningCount());
			for (var check : report.checks()) {
				if (check.violationCount() > 0) {
					log.info("Order reconciliation check: code={}, violations={}", check.code(), check.violationCount());
					List<ReconciliationFindingResponse> samples = report.findings().stream()
							.filter(finding -> finding.checkCode().equals(check.code())).limit(10).toList();
					for (ReconciliationFindingResponse finding : samples) {
						log.info("Order reconciliation finding: check={}, entityType={}, entityId={}, entityCode={}",
								finding.checkCode(), finding.entityType(), finding.entityId(), finding.entityCode());
					}
				}
			}
		} catch (Exception exception) {
			log.error("Order reconciliation startup check failed; application will continue", exception);
		}
	}
}
