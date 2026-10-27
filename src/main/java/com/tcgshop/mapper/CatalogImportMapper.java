package com.tcgshop.mapper;

import com.tcgshop.dto.request.CatalogImportRequest.Item;
import com.tcgshop.entity.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogImportMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "priceVnd")
    Product toProduct(Item item);
}
