package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.PromotionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketingExpirationServiceImplTest {

	@Mock
	private PromotionRepository promotionRepository;

	@Mock
	private CouponRepository couponRepository;

	@InjectMocks
	private MarketingExpirationServiceImpl service;

	@Test
	void deactivatesExpiredPromotionsAndCouponsUsingOneCutoff() {
		when(promotionRepository.deactivateExpiredPromotions(
				eq(ActiveStatus.ACTIVE), eq(ActiveStatus.INACTIVE), any()))
				.thenReturn(2);
		when(couponRepository.deactivateExpiredCoupons(
				eq(ActiveStatus.ACTIVE), eq(ActiveStatus.INACTIVE), any()))
				.thenReturn(3);

		LocalDateTime beforeRun = LocalDateTime.now();
		service.expireEndedMarketingPrograms();
		LocalDateTime afterRun = LocalDateTime.now();

		ArgumentCaptor<LocalDateTime> promotionCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> couponCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(promotionRepository).deactivateExpiredPromotions(
				eq(ActiveStatus.ACTIVE), eq(ActiveStatus.INACTIVE), promotionCutoff.capture());
		verify(couponRepository).deactivateExpiredCoupons(
				eq(ActiveStatus.ACTIVE), eq(ActiveStatus.INACTIVE), couponCutoff.capture());
		assertFalse(promotionCutoff.getValue().isBefore(beforeRun));
		assertFalse(promotionCutoff.getValue().isAfter(afterRun));
		org.junit.jupiter.api.Assertions.assertEquals(promotionCutoff.getValue(), couponCutoff.getValue());
	}
}
