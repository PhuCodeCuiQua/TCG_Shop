package com.tcgshop.mapper;

import com.tcgshop.dto.response.WishlistProductResponse;
import com.tcgshop.repository.WishlistProductProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WishlistMapper {
	WishlistProductResponse toResponse(WishlistProductProjection projection);
}
