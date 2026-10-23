package com.tcgshop.mapper;

import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface AddressMapper {

	@Mapping(target = "fullAddress", expression = "java(fullAddress(address))")
	@Mapping(target = "isDefault", expression = "java(address.isDefault())")
	AddressResponse toResponse(Address address);

	default String fullAddress(Address address) {
		return String.join(", ", address.getDetailAddress(), address.getWard(),
				address.getDistrict(), address.getProvince());
	}
}
