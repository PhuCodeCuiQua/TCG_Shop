package com.tcgshop.mapper;

import com.tcgshop.dto.response.PublicCatalogProductResponse;
import com.tcgshop.repository.PublicCatalogProductProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PublicCatalogProductMapper {

	PublicCatalogProductResponse toResponse(PublicCatalogProductProjection projection);
}
