package com.tcgshop.mapper;

import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.dto.response.ShopOptionResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductModerationMapper {

	default ProductModerationResponse toResponse(Product product) {
		return toResponse(product, com.tcgshop.util.ProductImageUtil.thumbnail(product));
	}

	@Mapping(target = "shopId", source = "product.shop.id")
	@Mapping(target = "shopName", source = "product.shop.name")
	@Mapping(target = "imageUrl", source = "imageUrl")
	ProductModerationResponse toResponse(Product product, String imageUrl);

	ShopOptionResponse toShopOption(Shop shop);
}
