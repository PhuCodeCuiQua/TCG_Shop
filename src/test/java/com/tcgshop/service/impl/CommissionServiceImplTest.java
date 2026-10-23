package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.CommissionRateRequest;
import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.CommissionSettingMapper;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommissionServiceImplTest {
	@Mock private ShopRepository shopRepository;
	@Mock private CommissionSettingRepository settingRepository;
	@Mock private UserRepository userRepository;
	@Mock private CommissionSettingMapper commissionSettingMapper;
	@Mock private CurrentUser currentUser;

	private CommissionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CommissionServiceImpl(
				shopRepository, settingRepository, userRepository, commissionSettingMapper, currentUser);
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

	@Test
	void getRate_missingShop_throwsNotFound() {
		when(shopRepository.findById(4L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getRate(4L));
	}

	@Test
	void addDefaultRate_appendsNewSettingWithCurrentAdmin() {
		User admin = new User();
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(currentUser.getId()).thenReturn(10L);
		when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
		CommissionSetting previous = new CommissionSetting();
		previous.setDefaultRate(new BigDecimal("5.00"));

		service.addDefaultRate(request("6.75"));

		ArgumentCaptor<CommissionSetting> captor = ArgumentCaptor.forClass(CommissionSetting.class);
		verify(settingRepository).save(captor.capture());
		assertEquals(new BigDecimal("6.75"), captor.getValue().getDefaultRate());
		assertEquals(admin, captor.getValue().getUpdatedBy());
		assertNotSame(previous, captor.getValue());
		assertEquals(new BigDecimal("5.00"), previous.getDefaultRate());
	}

	@Test
	void addDefaultRate_rateAbove100_rejectsWithoutSaving() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);

		assertThrows(BadRequestException.class, () -> service.addDefaultRate(request("100.01")));

		verify(settingRepository, never()).save(any());
	}

	@Test
	void addDefaultRate_nonAdmin_isForbidden() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThrows(ForbiddenException.class, () -> service.addDefaultRate(request("6.00")));

		verify(settingRepository, never()).save(any());
	}

	@Test
	void setShopRate_updatesExistingShopOverride() {
		Shop shop = new Shop();
		shop.setDeleted(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(shopRepository.findById(5L)).thenReturn(Optional.of(shop));

		service.setShopRate(5L, request("8.25"));

		assertEquals(new BigDecimal("8.25"), shop.getCommissionRate());
	}

	@Test
	void resetShopRate_clearsOverrideAndInheritsDefault() {
		Shop shop = new Shop();
		shop.setDeleted(false);
		shop.setCommissionRate(new BigDecimal("8.25"));
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(shopRepository.findById(5L)).thenReturn(Optional.of(shop));

		service.resetShopRate(5L);

		assertNull(shop.getCommissionRate());
	}

	@Test
	void setShopRate_missingShop_throwsNotFound() {
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(shopRepository.findById(6L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.setShopRate(6L, request("8.25")));
	}

	private CommissionRateRequest request(String rate) {
		CommissionRateRequest request = new CommissionRateRequest();
		request.setRate(new BigDecimal(rate));
		return request;
	}
}
