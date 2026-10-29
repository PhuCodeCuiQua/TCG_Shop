package com.tcgshop.mapper;

import com.tcgshop.dto.response.ReviewResponse;
import com.tcgshop.dto.response.ReviewProductResponse;
import com.tcgshop.dto.response.ReviewMediaResponse;
import com.tcgshop.entity.Review;
import com.tcgshop.entity.Product;
import com.tcgshop.repository.ReviewMediaListingRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
	@Mapping(target = "reviewerName", source = "user.fullName")
	@Mapping(target = "reviewerAvatarUrl", source = "user.avatarUrl")
	@Mapping(target = "reviewerId", source = "user.id")
	@Mapping(target = "media", ignore = true)
	@Mapping(target = "replies", ignore = true)
	ReviewResponse toResponse(Review review);

	default ReviewResponse toResponse(Review review, java.util.List<ReviewMediaResponse> media,
			java.util.List<com.tcgshop.dto.response.ReviewReplyMessageResponse> replies) {
		ReviewResponse response = toResponse(review);
		return new ReviewResponse(response.id(), response.reviewerId(), response.reviewerName(), response.reviewerAvatarUrl(),
				response.rating(), response.comment(), response.createdAt(), media, replies);
	}

	@org.mapstruct.Mapping(target = "mediaUrl", source = "mediaUrl")
	@org.mapstruct.Mapping(target = "mediaType", source = "mediaType")
	ReviewMediaResponse toMediaResponse(ReviewMediaListingRow media);

	@Mapping(target = "averageRating", expression = "java(product.getAvgRating() == null ? java.math.BigDecimal.ZERO : product.getAvgRating())")
	@Mapping(target = "reviewCount", expression = "java(product.getRatingCount() == null ? 0 : product.getRatingCount())")
	@Mapping(target = "imageUrl", expression = "java(com.tcgshop.util.ProductImageUtil.thumbnail(product))")
	@Mapping(target = "price", ignore = true)
	ReviewProductResponse toProductResponse(Product product);
}
