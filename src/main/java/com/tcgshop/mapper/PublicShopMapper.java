package com.tcgshop.mapper;

import com.tcgshop.dto.response.PublicShopResponse;
import com.tcgshop.entity.Shop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PublicShopMapper {

	PublicShopResponse toResponse(Shop shop);
}
