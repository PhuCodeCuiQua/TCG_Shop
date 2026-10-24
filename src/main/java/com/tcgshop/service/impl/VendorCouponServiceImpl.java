package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorCouponRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.VendorCouponResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorCouponMapper;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorCouponRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.VendorCouponService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorCouponServiceImpl implements VendorCouponService {
	private final VendorCouponRepository vendorCouponRepository;
	private final CouponRepository couponRepository;
	private final ShopRepository shopRepository;
	private final VendorCouponMapper mapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<VendorCouponResponse> getCurrentShopCoupons(Pageable pageable) {
		Shop shop = getCurrentShop();
		return PageResponse.from(vendorCouponRepository.findAllByShopId(shop.getId(), pageable), this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public VendorCouponResponse getCurrentShopCoupon(Long couponId) {
		return toResponse(findOwnedCoupon(couponId));
	}

	@Override
	@Transactional
	public VendorCouponResponse saveCurrentShopCoupon(Long couponId, VendorCouponRequest request) {
		validateRequest(request);
		Shop shop = getCurrentShop();
		Coupon coupon = couponId == null ? new Coupon() : findOwnedCoupon(couponId);
		String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
		boolean duplicateCode = couponId == null
				? couponRepository.existsByCodeIgnoreCase(normalizedCode)
				: couponRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, couponId);
		if (duplicateCode) {
			throw new ConflictException("Mã giảm giá đã tồn tại trên hệ thống");
		}
		if (coupon.getUsedQuantity() > 0 && request.getTotalQuantity() < coupon.getUsedQuantity()) {
			throw new BadRequestException("Tổng lượt sử dụng không thể nhỏ hơn số lượt đã dùng");
		}

		coupon.setShop(shop);
		coupon.setCode(normalizedCode);
		coupon.setDiscountType(request.getDiscountType());
		coupon.setDiscountValue(request.getDiscountValue());
		coupon.setMinOrderValue(request.getMinOrderValue() == null ? BigDecimal.ZERO : request.getMinOrderValue());
		coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
		coupon.setTotalQuantity(request.getTotalQuantity());
		coupon.setLimitPerUser(request.getLimitPerUser());
		coupon.setStartDate(request.getStartDate());
		coupon.setEndDate(request.getEndDate());
		coupon.setStatus(request.getStatus());
		return toResponse(vendorCouponRepository.save(coupon));
	}

	@Override
	@Transactional
	public VendorCouponResponse toggleCurrentShopCoupon(Long couponId) {
		Coupon coupon = findOwnedCoupon(couponId);
		coupon.setStatus(coupon.getStatus() == ActiveStatus.ACTIVE ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
		return toResponse(coupon);
	}

	@Override
	@Transactional
	public VendorCouponResponse deactivateCurrentShopCoupon(Long couponId) {
		Coupon coupon = findOwnedCoupon(couponId);
		coupon.setStatus(ActiveStatus.INACTIVE);
		return toResponse(coupon);
	}

	private void validateRequest(VendorCouponRequest request) {
		if (request == null || request.getCode() == null || request.getCode().isBlank()
				|| request.getDiscountType() == null || request.getDiscountValue() == null
				|| request.getTotalQuantity() == null || request.getLimitPerUser() == null
				|| request.getStartDate() == null || request.getEndDate() == null || request.getStatus() == null) {
			throw new BadRequestException("Vui lòng nhập đầy đủ thông tin mã giảm giá");
		}
		if (!request.getEndDate().isAfter(request.getStartDate())) {
			throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
		}
		if (request.getDiscountValue().signum() <= 0
				|| request.getMinOrderValue() != null && request.getMinOrderValue().signum() < 0
				|| request.getMaxDiscountAmount() != null && request.getMaxDiscountAmount().signum() <= 0) {
			throw new BadRequestException("Giá trị giảm phải lớn hơn 0; đơn tối thiểu không được âm");
		}
		if (request.getDiscountType() == DiscountType.PERCENTAGE
				&& request.getDiscountValue().compareTo(new BigDecimal("100.00")) > 0) {
			throw new BadRequestException("Mức giảm theo phần trăm không được vượt quá 100%");
		}
		if (request.getTotalQuantity() < 1 || request.getLimitPerUser() < 1) {
			throw new BadRequestException("Tổng lượt và giới hạn mỗi người phải lớn hơn 0");
		}
	}

	private Coupon findOwnedCoupon(Long couponId) {
		Shop shop = getCurrentShop();
		Coupon coupon = vendorCouponRepository.findByIdWithShop(couponId)
				.orElseGet(() -> vendorCouponRepository.findById(couponId)
						.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá")));
		if (coupon.getShop() == null || !shop.getId().equals(coupon.getShop().getId())) {
			throw new ForbiddenException("Bạn không có quyền quản lý mã giảm giá của shop khác");
		}
		return coupon;
	}

	private Shop getCurrentShop() {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ Vendor mới được quản lý mã giảm giá shop");
		}
		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop của tài khoản hiện tại"));
		if (shop.getStatus() != ShopStatus.APPROVED) {
			throw new ForbiddenException("Chỉ shop đã được duyệt mới được quản lý mã giảm giá");
		}
		return shop;
	}

	private VendorCouponResponse toResponse(Coupon coupon) {
		VendorCouponResponse response = mapper.toResponse(coupon);
		String displayStatus;
		if (coupon.getStatus() == ActiveStatus.INACTIVE) {
			displayStatus = "Tạm dừng";
		} else if (LocalDateTime.now().isBefore(coupon.getStartDate())) {
			displayStatus = "Sắp diễn ra";
		} else if (LocalDateTime.now().isAfter(coupon.getEndDate())) {
			displayStatus = "Hết hạn";
		} else {
			displayStatus = "Hoạt động";
		}
		return new VendorCouponResponse(response.id(), response.code(), response.discountType(), response.discountValue(),
				response.minOrderValue(), response.maxDiscountAmount(), response.totalQuantity(), response.usedQuantity(),
				response.limitPerUser(), response.startDate(), response.endDate(), response.status(), displayStatus);
	}
}
