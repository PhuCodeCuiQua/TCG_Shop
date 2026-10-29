package com.tcgshop.service.impl;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.service.MarketingExpirationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketingExpirationServiceImpl implements MarketingExpirationService {

	private final PromotionRepository promotionRepository;
	private final CouponRepository couponRepository;

	@Override
	@Transactional
	public void expireEndedMarketingPrograms() {
		LocalDateTime now = LocalDateTime.now();
		int expiredPromotions = promotionRepository.deactivateExpiredPromotions(
				ActiveStatus.ACTIVE, ActiveStatus.INACTIVE, now);
		int expiredCoupons = couponRepository.deactivateExpiredCoupons(
				ActiveStatus.ACTIVE, ActiveStatus.INACTIVE, now);
		if (expiredPromotions > 0 || expiredCoupons > 0) {
			log.info("Deactivated expired marketing programs: promotions={}, coupons={}",
					expiredPromotions, expiredCoupons);
		}
	}
}
