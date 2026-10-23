package com.tcgshop.mapper;

import com.tcgshop.dto.response.ShopReviewResponse;
import com.tcgshop.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShopReviewMapper {

	@Mapping(target = "ownerId", source = "owner.id")
	@Mapping(target = "ownerEmail", source = "owner.email")
	@Mapping(target = "ownerFullName", source = "owner.fullName")
	ShopReviewResponse toResponse(Shop shop);
}
