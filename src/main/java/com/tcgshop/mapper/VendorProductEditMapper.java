package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorProductEditResponse;
import com.tcgshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorProductEditMapper {
	@Mapping(target = "imageUrl", expression = "java(primaryImageUrl(product))")
	VendorProductEditResponse toResponse(Product product);

	default String primaryImageUrl(Product product) {
		return com.tcgshop.util.ProductImageUtil.thumbnail(product);
	}
}
