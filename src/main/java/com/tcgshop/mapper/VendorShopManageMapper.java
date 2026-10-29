package com.tcgshop.mapper;

import com.tcgshop.dto.request.VendorShopManageRequest;
import com.tcgshop.dto.response.VendorShopManageResponse;
import com.tcgshop.entity.Shop;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VendorShopManageMapper {

	@Mapping(target = "description", expression = "java(clean(request.getDescription()))")
	@Mapping(target = "phoneNumber", expression = "java(clean(request.getPhoneNumber()))")
	@Mapping(target = "pickupAddress", expression = "java(clean(request.getPickupAddress()))")
	@BeanMapping(ignoreByDefault = true)
	void updateShop(VendorShopManageRequest request, @MappingTarget Shop shop);

	VendorShopManageResponse toResponse(Shop shop);

	default String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
