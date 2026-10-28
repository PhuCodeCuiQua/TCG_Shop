package com.tcgshop.scheduler;

import com.tcgshop.service.MarketingExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketingExpirationJob {

	private final MarketingExpirationService marketingExpirationService;

	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
	public void expireEndedMarketingPrograms() {
		try {
			marketingExpirationService.expireEndedMarketingPrograms();
		} catch (RuntimeException exception) {
			log.error("Marketing expiration job failed", exception);
		}
	}
}
