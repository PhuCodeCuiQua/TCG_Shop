package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorPromotionProductOptionResponse;
import com.tcgshop.dto.response.VendorPromotionResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorPromotionMapper {
	@Mapping(target = "displayStatus", ignore = true)
	@Mapping(target = "productIds", expression = "java(promotion.getProducts().stream().map(product -> product.getId()).sorted().toList())")
	VendorPromotionResponse toResponse(Promotion promotion);

	@Mapping(target = "displayStatus", ignore = true)
	@Mapping(target = "productIds", ignore = true)
	VendorPromotionResponse toListResponse(Promotion promotion);

	VendorPromotionProductOptionResponse toProductOption(Product product);
}
