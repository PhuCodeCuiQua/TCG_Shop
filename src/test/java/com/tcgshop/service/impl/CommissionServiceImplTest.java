package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Shop;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.ShopRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommissionServiceImplTest {
	@Mock private ShopRepository shopRepository;
	@Mock private CommissionSettingRepository settingRepository;

	private CommissionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CommissionServiceImpl(shopRepository, settingRepository);
	}

	@Test
	void prefersShopSpecificCommissionRate() {
		Shop shop = new Shop();
		shop.setCommissionRate(new BigDecimal("7.50"));
		when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

		assertEquals(new BigDecimal("7.50"), service.getRate(1L));
		verify(settingRepository, never()).findTopByOrderByCreatedAtDesc();
	}

	@Test
	void usesLatestDefaultCommissionSettingWhenShopRateIsNull() {
		Shop shop = new Shop();
		CommissionSetting setting = new CommissionSetting();
		setting.setDefaultRate(new BigDecimal("6.25"));
		when(shopRepository.findById(2L)).thenReturn(Optional.of(shop));
		when(settingRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.of(setting));

		assertEquals(new BigDecimal("6.25"), service.getRate(2L));
	}

	@Test
	void fallsBackToFivePercentWhenNoSettingExists() {
		Shop shop = new Shop();
		when(shopRepository.findById(3L)).thenReturn(Optional.of(shop));
		when(settingRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

		assertEquals(new BigDecimal("5.00"), service.getRate(3L));
	}
}
