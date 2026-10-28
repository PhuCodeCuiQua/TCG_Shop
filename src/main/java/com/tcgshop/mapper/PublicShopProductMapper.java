package com.tcgshop.mapper;

import com.tcgshop.dto.response.PublicShopProductResponse;
import com.tcgshop.repository.PublicShopProductProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PublicShopProductMapper {

	@Mapping(target = "originalPrice", source = "price")
	@Mapping(target = "effectivePrice", source = "price")
	PublicShopProductResponse toResponse(PublicShopProductProjection projection);
}
