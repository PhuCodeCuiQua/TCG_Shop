package com.tcgshop.mapper;

import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.dto.response.ShopOptionResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductModerationMapper {

	@Mapping(target = "shopId", source = "shop.id")
	@Mapping(target = "shopName", source = "shop.name")
	ProductModerationResponse toResponse(Product product);

	ShopOptionResponse toShopOption(Shop shop);
}
