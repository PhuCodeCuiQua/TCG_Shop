package com.tcgshop.mapper;

import com.tcgshop.dto.response.PublicShopProductResponse;
import com.tcgshop.repository.PublicShopProductProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PublicShopProductMapper {

	PublicShopProductResponse toResponse(PublicShopProductProjection projection);
}
