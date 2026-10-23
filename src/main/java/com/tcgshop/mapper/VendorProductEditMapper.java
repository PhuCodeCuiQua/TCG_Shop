package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorProductEditResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorProductEditMapper {
	@Mapping(target = "imageUrl", expression = "java(primaryImageUrl(product))")
	VendorProductEditResponse toResponse(Product product);

	default String primaryImageUrl(Product product) {
		if (product.getImages() == null || product.getImages().isEmpty()) return null;
		return product.getImages().stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsMain()))
				.findFirst()
				.or(() -> product.getImages().stream().findFirst())
				.map(ProductImage::getImageUrl)
				.orElse(null);
	}
}
