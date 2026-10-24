package com.tcgshop.mapper;

import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.repository.PublicProductSearchProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PublicProductSearchMapper {

	PublicProductSearchItemResponse toResponse(PublicProductSearchProjection projection);
}
