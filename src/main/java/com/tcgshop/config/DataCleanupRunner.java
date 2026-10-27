package com.tcgshop.config;

import com.tcgshop.service.DataCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.data-cleanup.enabled", havingValue = "true")
public class DataCleanupRunner implements CommandLineRunner {
	private final DataCleanupService dataCleanupService;

	@Value("${app.data-cleanup.apply:false}")
	private boolean apply;

	@Override
	public void run(String... args) {
		DataCleanupService.CleanupResult result = dataCleanupService.cleanupProductsWithoutRealImages(apply);
		if (!result.catalogAssetsLoaded()) {
			log.warn("Product cleanup did not run because catalog assets could not be verified");
			return;
		}
		if (apply) {
			log.info("Product cleanup applied: {} candidates, {} products soft-deleted; product IDs={}",
					result.candidates(), result.hidden(), result.candidateIds());
		} else {
			log.info("Product cleanup dry-run: {} candidates. Review the count, then enable "
					+ "app.data-cleanup.apply=true to soft-delete them; candidate product IDs={}",
					result.candidates(), result.candidateIds());
		}
	}
}
