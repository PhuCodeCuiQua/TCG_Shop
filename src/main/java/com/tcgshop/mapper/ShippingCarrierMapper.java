package com.tcgshop.mapper;

import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.ShippingCarrier;
import org.mapstruct.Mapper;

@Mapper(config = CommonMapperConfig.class)
public interface ShippingCarrierMapper {
	ShippingCarrierResponse toResponse(ShippingCarrier carrier);
}
