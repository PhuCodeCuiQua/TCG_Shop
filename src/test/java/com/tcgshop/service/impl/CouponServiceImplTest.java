package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.CouponApplyResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.CouponUsageRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.VendorCouponRepository;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {
	private static final Long USER_ID = 17L;
	private static final Long COUPON_ID = 41L;

	@Mock private CouponRepository couponRepository;
	@Mock private VendorCouponRepository vendorCouponRepository;
	@Mock private CouponUsageRepository couponUsageRepository;
	@Mock private UserRepository userRepository;
	@Mock private OrderRepository orderRepository;
	@Mock private CurrentUser currentUser;
	private CouponServiceImpl service;
	private Coupon coupon;

	@BeforeEach
	void setUp() {
		service = new CouponServiceImpl(couponRepository, vendorCouponRepository,
				couponUsageRepository, userRepository, orderRepository, currentUser);
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(USER_ID);

		LocalDateTime now = LocalDateTime.now();
		coupon = new Coupon();
		coupon.setId(COUPON_ID);
		coupon.setCode("SAVE");
		coupon.setStatus(ActiveStatus.ACTIVE);
		coupon.setStartDate(now.minusDays(1));
		coupon.setEndDate(now.plusDays(1));
		coupon.setDiscountType(DiscountType.PERCENTAGE);
		coupon.setDiscountValue(new BigDecimal("25"));
		coupon.setMinOrderValue(BigDecimal.ZERO);
		coupon.setTotalQuantity(100);
		coupon.setUsedQuantity(0);
		coupon.setLimitPerUser(1);
		when(couponRepository.findByCode("SAVE")).thenReturn(Optional.of(coupon));
	}

	@Test
	void rejectsExpiredCoupon() {
		coupon.setEndDate(LocalDateTime.now().minusMinutes(1));

		CouponApplyResponse result = service.validateAndCalculate("save", USER_ID, null, new BigDecimal("1000"));

		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("hết hạn");
	}

	@Test
	void rejectsCouponWithNoRemainingUses() {
		coupon.setUsedQuantity(coupon.getTotalQuantity());

		CouponApplyResponse result = service.validateAndCalculate("SAVE", USER_ID, null, new BigDecimal("1000"));

		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("hết lượt");
	}

	@Test
	void rejectsSubtotalBelowMinimum() {
		coupon.setMinOrderValue(new BigDecimal("500"));

		CouponApplyResponse result = service.validateAndCalculate("SAVE", USER_ID, null, new BigDecimal("499"));

		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("giá trị tối thiểu");
	}

	@Test
	void acceptsPlatformCouponAndChecksMinimumAgainstFullCartSubtotal() {
		coupon.setMinOrderValue(new BigDecimal("200000"));
		when(couponUsageRepository.countByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(0L);

		CouponApplyResponse applicable = service.validateAndCalculate(
				"SAVE", USER_ID, null, new BigDecimal("200000"));
		CouponApplyResponse belowMinimum = service.validateAndCalculate(
				"SAVE", USER_ID, null, new BigDecimal("199999"));

		assertThat(applicable.valid()).isTrue();
		assertThat(applicable.discountAmount()).isEqualByComparingTo("50000");
		assertThat(belowMinimum.valid()).isFalse();
		assertThat(belowMinimum.message()).contains("giá trị tối thiểu");
	}

	@Test
	void doesNotApplyPlatformCouponToAShopScopedCheckoutRequest() {
		CouponApplyResponse result = service.validateAndCalculate(
				"SAVE", USER_ID, 12L, new BigDecimal("1000"));

		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("không áp dụng");
	}

	@Test
	void calculatesPercentageDiscountAndHonorsMaximumCap() {
		when(couponUsageRepository.countByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(0L);
		coupon.setMaxDiscountAmount(new BigDecimal("200"));

		CouponApplyResponse result = service.validateAndCalculate(" save ", USER_ID, null, new BigDecimal("1000"));

		assertThat(result.valid()).isTrue();
		assertThat(result.couponId()).isEqualTo(COUPON_ID);
		assertThat(result.discountAmount()).isEqualByComparingTo("200");
	}

	@Test
	void rejectsCouponAlreadyUsedByBuyer() {
		when(couponUsageRepository.countByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(1L);
		assertThat(service.validateAndCalculate("SAVE", USER_ID, null, new BigDecimal("1000")).valid()).isFalse();
	}
}
