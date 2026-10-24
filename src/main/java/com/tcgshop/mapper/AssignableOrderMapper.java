package com.tcgshop.mapper;

import com.tcgshop.dto.response.AssignableOrderResponse;
import com.tcgshop.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssignableOrderMapper {

	@Mapping(target = "shopName", source = "shop.name")
	@Mapping(target = "currentShipperId", source = "shipper.id")
	@Mapping(target = "currentShipperName", source = "shipper.fullName")
	AssignableOrderResponse toResponse(Order order);
}