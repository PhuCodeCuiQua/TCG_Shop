package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponSuggestionResponse(Long id, Long shopId, String code,
        DiscountType discountType, BigDecimal discountValue, BigDecimal minOrderValue,
        BigDecimal maxDiscountAmount, LocalDateTime endDate, BigDecimal discountAmount) { }
