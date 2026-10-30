package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.security.CurrentUser;
import org.junit.jupiter.api.Test;

class PlatformMarketingAuthorizationTest {
	@Test
	void managerCanCreateCouponsAndPromotions() {
		CurrentUser actor = mock(CurrentUser.class);
		when(actor.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		var promotions = new PlatformPromotionServiceImpl(null, null, null, null, null, actor, null);
		var coupons = new PlatformCouponAdminServiceImpl(null, null, actor);
		assertThatThrownBy(() -> promotions.save(null, null)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> coupons.save(null, null)).isInstanceOf(BadRequestException.class);
		// Manager có ID cập nhật được qua bước phân quyền và tới validation nghiệp vụ.
		assertThatThrownBy(() -> promotions.save(5L, null)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> coupons.save(5L, null)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void adminCanReachValidationForBothCreationAndUpdate() {
		CurrentUser actor = mock(CurrentUser.class);
		when(actor.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		var promotions = new PlatformPromotionServiceImpl(null, null, null, null, null, actor, null);
		var coupons = new PlatformCouponAdminServiceImpl(null, null, actor);
		for (Long id : new Long[] {null, 5L}) {
			assertThatThrownBy(() -> promotions.save(id, null)).isInstanceOf(BadRequestException.class);
			assertThatThrownBy(() -> coupons.save(id, null)).isInstanceOf(BadRequestException.class);
		}
	}
}
