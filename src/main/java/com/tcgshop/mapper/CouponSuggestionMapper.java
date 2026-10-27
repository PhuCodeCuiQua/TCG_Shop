package com.tcgshop.mapper;

import com.tcgshop.dto.response.CouponSuggestionResponse;
import com.tcgshop.entity.Coupon;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponSuggestionMapper {
    @Mapping(target = "shopId", source = "coupon.shop.id")
    @Mapping(target = "discountAmount", source = "discountAmount")
    CouponSuggestionResponse toResponse(Coupon coupon, BigDecimal discountAmount);
}
