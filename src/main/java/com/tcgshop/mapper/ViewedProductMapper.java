package com.tcgshop.mapper;

import com.tcgshop.dto.response.ViewedProductResponse;
import com.tcgshop.repository.ViewedProductProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ViewedProductMapper {
	ViewedProductResponse toResponse(ViewedProductProjection projection);
}
