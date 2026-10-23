package com.tcgshop.mapper;

import com.tcgshop.dto.response.CartItemResponse;
import com.tcgshop.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface CartMapper {

	@Mapping(target = "itemId", source = "id")
	@Mapping(target = "productId", source = "product.id")
	@Mapping(target = "productName", source = "product.name")
	@Mapping(target = "cardCondition", expression = "java(cartItem.getProduct().getCardCondition() == null ? null : cartItem.getProduct().getCardCondition().getDisplayName())")
	@Mapping(target = "originalPrice", source = "product.price")
	@Mapping(target = "quantityExceedsStock", ignore = true)
	@Mapping(target = "available", ignore = true)
	@Mapping(target = "unavailableReason", ignore = true)
	@Mapping(target = "priceChanged", ignore = true)
	@Mapping(target = "previousPrice", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "unitPrice", ignore = true)
	@Mapping(target = "lineTotal", ignore = true)
	@Mapping(target = "stock", source = "product.stock")
	CartItemResponse toResponse(CartItem cartItem);
}
