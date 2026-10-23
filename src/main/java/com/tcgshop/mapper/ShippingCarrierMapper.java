package com.tcgshop.mapper;

import com.tcgshop.dto.response.AdminShippingCarrierResponse;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.ShippingCarrier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface ShippingCarrierMapper {
	ShippingCarrierResponse toResponse(ShippingCarrier carrier);

	@Mapping(target = "usedOrderCount", source = "usedOrderCount")
	AdminShippingCarrierResponse toAdminResponse(ShippingCarrier carrier, long usedOrderCount);
}
