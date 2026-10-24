package com.tcgshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.reconcile")
public record ReconciliationProperties(
		@DefaultValue("false") boolean runOnStartup,
		@DefaultValue("50") int maxFindingsPerCheck) {
}
