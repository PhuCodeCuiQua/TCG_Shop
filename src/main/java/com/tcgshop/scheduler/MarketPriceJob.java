package com.tcgshop.scheduler;

import com.tcgshop.service.MarketPriceJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketPriceJob {
	private final MarketPriceJobService marketPriceJobService;

	@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
	public void calculateNightlyMarketPrices() {
		try {
			marketPriceJobService.runNow();
		} catch (RuntimeException exception) {
			log.error("Market price job failed", exception);
		}
	}

}
