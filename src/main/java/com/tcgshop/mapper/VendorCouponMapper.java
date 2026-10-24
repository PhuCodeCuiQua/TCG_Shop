package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorCouponResponse;
import com.tcgshop.entity.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorCouponMapper {
	@Mapping(target = "displayStatus", ignore = true)
	VendorCouponResponse toResponse(Coupon coupon);
}
