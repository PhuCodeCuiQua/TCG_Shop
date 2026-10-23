package com.tcgshop.mapper;

import com.tcgshop.dto.response.PlatformCouponResponse;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlatformMarketingMapper {

	@Mapping(target = "gameId", source = "game.id")
	@Mapping(target = "gameName", source = "game.name")
	@Mapping(target = "seriesId", source = "series.id")
	@Mapping(target = "seriesName", source = "series.name")
	@Mapping(target = "productIds", expression = "java(promotion.getProducts().stream().map(product -> product.getId()).toList())")
	@Mapping(target = "targetSummary", ignore = true)
	PlatformPromotionResponse toPromotionResponse(Promotion promotion);

	@Mapping(target = "productIds", ignore = true)
	@Mapping(target = "targetSummary", ignore = true)
	PlatformPromotionResponse toPromotionListResponse(Promotion promotion);

	PlatformCouponResponse toCouponResponse(Coupon coupon);
}
