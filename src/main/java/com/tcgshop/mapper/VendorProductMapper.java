package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorProductResponse;
import com.tcgshop.repository.VendorProductProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorProductMapper {

	VendorProductResponse toResponse(VendorProductProjection projection);
}
