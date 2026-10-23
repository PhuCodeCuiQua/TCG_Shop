package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorSingleProductResponse;
import com.tcgshop.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorSingleProductMapper {

	VendorSingleProductResponse toResponse(Product product);
}
