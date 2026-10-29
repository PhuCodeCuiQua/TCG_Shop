package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PlatformCouponRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PlatformCouponResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PlatformMarketingMapper;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PlatformCouponAdminService;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PlatformCouponAdminServiceImpl implements PlatformCouponAdminService {

	private final CouponRepository couponRepository;
	private final PlatformMarketingMapper marketingMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PlatformCouponResponse> search(ActiveStatus status, Pageable pageable) {
		ensureManagerOrAdmin();
		return PageResponse.from(
				couponRepository.searchPlatformCoupons(status, pageable),
				marketingMapper::toCouponResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public PlatformCouponResponse getById(Long couponId) {
		ensureManagerOrAdmin();
		return marketingMapper.toCouponResponse(findPlatformCoupon(couponId));
	}

	@Override
	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public PlatformCouponResponse save(Long couponId, PlatformCouponRequest request) {
		ensureManagerOrAdmin();
		if (couponId == null && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Chỉ Admin được tạo mới chương trình hệ thống");
		}
		validateRequest(request);
		String code = request.getCode().trim().toUpperCase(Locale.ROOT);
		boolean duplicate = couponId == null
				? couponRepository.existsByCodeIgnoreCase(code)
				: couponRepository.existsByCodeIgnoreCaseAndIdNot(code, couponId);
		if (duplicate) {
			throw new ConflictException("Mã giảm giá đã tồn tại");
		}

		Coupon coupon = couponId == null ? new Coupon() : findPlatformCoupon(couponId);
		if (coupon.getUsedQuantity() > 0 && request.getTotalQuantity() != null
				&& request.getTotalQuantity() < coupon.getUsedQuantity()) {
			throw new BadRequestException("Tổng lượt sử dụng không thể nhỏ hơn số lượt đã dùng");
		}
		coupon.setShop(null);
		coupon.setCode(code);
		coupon.setDiscountType(request.getDiscountType());
		coupon.setDiscountValue(request.getDiscountValue());
		coupon.setMinOrderValue(request.getMinOrderValue() == null ? BigDecimal.ZERO : request.getMinOrderValue());
		coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
		coupon.setTotalQuantity(request.getTotalQuantity());
		coupon.setLimitPerUser(request.getLimitPerUser() == null ? 1 : request.getLimitPerUser());
		coupon.setStartDate(request.getStartDate());
		coupon.setEndDate(request.getEndDate());
		coupon.setStatus(request.getStatus());
		return marketingMapper.toCouponResponse(couponRepository.save(coupon));
	}

	@Override
	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public PlatformCouponResponse setStatus(Long couponId, ActiveStatus status) {
		ensureManagerOrAdmin();
		if (status == null) {
			throw new BadRequestException("Vui lòng chọn trạng thái");
		}
		Coupon coupon = findPlatformCoupon(couponId);
		coupon.setStatus(status);
		return marketingMapper.toCouponResponse(coupon);
	}

	private void validateRequest(PlatformCouponRequest request) {
		if (request == null || request.getCode() == null || request.getCode().isBlank()
				|| request.getDiscountType() == null || request.getDiscountValue() == null
				|| request.getStartDate() == null || request.getEndDate() == null
				|| request.getStatus() == null) {
			throw new BadRequestException("Vui lòng nhập đầy đủ thông tin mã giảm giá");
		}
		if (!request.getStartDate().isBefore(request.getEndDate())) {
			throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
		}
		if (request.getDiscountValue().signum() <= 0
				|| request.getMinOrderValue() != null && request.getMinOrderValue().signum() < 0
				|| request.getMaxDiscountAmount() != null && request.getMaxDiscountAmount().signum() <= 0) {
			throw new BadRequestException("Giá trị giảm và mức giảm tối đa phải lớn hơn 0; đơn tối thiểu không được âm");
		}
		if (request.getDiscountType() == DiscountType.PERCENTAGE
				&& request.getDiscountValue().compareTo(new BigDecimal("100.00")) > 0) {
			throw new BadRequestException("Mã giảm theo phần trăm không được vượt quá 100%");
		}
		if (request.getTotalQuantity() != null && request.getTotalQuantity() < 1) {
			throw new BadRequestException("Tổng lượt sử dụng phải lớn hơn 0");
		}
		if (request.getLimitPerUser() != null && request.getLimitPerUser() < 1) {
			throw new BadRequestException("Giới hạn mỗi người phải lớn hơn 0");
		}
	}

	private Coupon findPlatformCoupon(Long couponId) {
		return couponRepository.findPlatformCouponById(couponId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá hệ thống"));
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền xem mã giảm giá hệ thống");
		}
	}

}
