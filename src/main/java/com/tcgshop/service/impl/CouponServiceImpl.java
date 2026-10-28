package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CouponApplyResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.CouponUsage;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.CouponUsageRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.VendorCouponRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CouponService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final CouponRepository couponRepository;
	private final VendorCouponRepository vendorCouponRepository;
	private final CouponUsageRepository couponUsageRepository;
	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public CouponApplyResponse validateAndCalculate(String code, Long userId, Long shopId, BigDecimal subtotal) {
		if (code == null || code.isBlank() || userId == null || subtotal == null || subtotal.signum() < 0) {
			return invalid("Thông tin mã giảm giá không hợp lệ");
		}
		if (!currentUser.isAuthenticated() || !userId.equals(currentUser.getId())) {
			throw new ForbiddenException("Không thể kiểm tra mã giảm giá cho tài khoản khác");
		}
		String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
		Coupon coupon = couponRepository.findByCode(normalizedCode).orElse(null);
		if (coupon == null) return invalid("Mã giảm giá không tồn tại");

		LocalDateTime now = LocalDateTime.now();
		if (coupon.getStatus() != ActiveStatus.ACTIVE) return invalid("Mã giảm giá đang tạm dừng");
		if (coupon.getStartDate() == null || now.isBefore(coupon.getStartDate())) {
			return invalid("Mã giảm giá chưa đến thời gian sử dụng");
		}
		if (coupon.getEndDate() == null || now.isAfter(coupon.getEndDate())) return invalid("Mã đã hết hạn");
		if (coupon.getTotalQuantity() != null && coupon.getUsedQuantity() >= coupon.getTotalQuantity()) {
			return invalid("Mã giảm giá đã hết lượt sử dụng");
		}
		if (!isScopeMatching(coupon, shopId)) return invalid("Mã giảm giá không áp dụng cho chi nhánh này");
		if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
			return invalid("Đơn hàng chưa đạt giá trị tối thiểu");
		}
		long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
		if (userUsageCount >= coupon.getLimitPerUser()) return invalid("Bạn đã sử dụng mã vượt quá số lần cho phép");

		return new CouponApplyResponse(true, coupon.getId(), calculateDiscount(coupon, subtotal), "Áp dụng mã giảm giá thành công");
	}

	@Override
	@Transactional
	public void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount) {
		if (couponId == null || userId == null || orderId == null || discountAmount == null || discountAmount.signum() < 0) {
			throw new BadRequestException("Thông tin ghi nhận mã giảm giá không hợp lệ");
		}
		if (!currentUser.isAuthenticated() || !userId.equals(currentUser.getId())) {
			throw new ForbiddenException("Không thể ghi nhận mã giảm giá cho tài khoản khác");
		}
		Coupon coupon = vendorCouponRepository.findByIdForUpdate(couponId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
		if (order.getUser() == null || !userId.equals(order.getUser().getId())) {
			throw new ForbiddenException("Đơn hàng không thuộc tài khoản đang đăng nhập");
		}
		if (coupon.getShop() != null && (order.getShop() == null || !coupon.getShop().getId().equals(order.getShop().getId()))) {
			throw new BadRequestException("Mã giảm giá không áp dụng cho chi nhánh của đơn hàng");
		}
		if (vendorCouponRepository.countUsageForCouponAndOrder(couponId, orderId) > 0) return;

		LocalDateTime now = LocalDateTime.now();
		if (coupon.getStatus() != ActiveStatus.ACTIVE || coupon.getStartDate() == null || coupon.getEndDate() == null
				|| now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
			throw new BadRequestException("Mã giảm giá không còn hiệu lực");
		}
		if (coupon.getTotalQuantity() != null && coupon.getUsedQuantity() >= coupon.getTotalQuantity()) {
			throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
		}
		if (couponUsageRepository.countByCouponIdAndUserId(couponId, userId) >= coupon.getLimitPerUser()) {
			throw new BadRequestException("Bạn đã sử dụng mã vượt quá số lần cho phép");
		}

		CouponUsage usage = new CouponUsage();
		usage.setCoupon(coupon);
		usage.setUser(user);
		usage.setOrder(order);
		usage.setDiscountAmount(discountAmount);
		usage.setUsedAt(now);
		coupon.setUsedQuantity(coupon.getUsedQuantity() + 1);
		couponUsageRepository.save(usage);
		log.debug("Recorded coupon usage couponId={} userId={} orderId={}", couponId, userId, orderId);
	}

	private CouponApplyResponse invalid(String message) {
		log.debug("Coupon validation rejected: {}", message);
		return new CouponApplyResponse(false, null, ZERO, message);
	}

	private boolean isScopeMatching(Coupon coupon, Long shopId) {
		if (coupon.getShop() == null) return shopId == null;
		return shopId != null && shopId.equals(coupon.getShop().getId());
	}

	private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
		BigDecimal amount = coupon.getDiscountType() == DiscountType.PERCENTAGE
				? subtotal.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
				: coupon.getDiscountValue();
		if (coupon.getDiscountType() == DiscountType.PERCENTAGE && coupon.getMaxDiscountAmount() != null) {
			amount = amount.min(coupon.getMaxDiscountAmount());
		}
		return amount.min(subtotal).max(ZERO);
	}
}
