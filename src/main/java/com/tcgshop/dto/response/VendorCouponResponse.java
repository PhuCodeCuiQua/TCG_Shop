package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorCouponResponse(
		Long id,
		String code,
		DiscountType discountType,
		BigDecimal discountValue,
		BigDecimal minOrderValue,
		BigDecimal maxDiscountAmount,
		Integer totalQuantity,
		int usedQuantity,
		int limitPerUser,
		LocalDateTime startDate,
		LocalDateTime endDate,
		ActiveStatus status,
		String displayStatus) {
}
