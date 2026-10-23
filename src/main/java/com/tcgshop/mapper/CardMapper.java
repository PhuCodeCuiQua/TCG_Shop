package com.tcgshop.mapper;

import com.tcgshop.dto.request.CardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardResponse;
import com.tcgshop.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CardMapper {

	// Tên khác nhau nên khai báo @Mapping (mục 6)
	@Mapping(target = "seriesId", source = "series.id")
	@Mapping(target = "seriesName", source = "series.name")
	@Mapping(target = "gameId", source = "series.game.id")
	@Mapping(target = "gameName", source = "series.game.name")
	CardResponse toResponse(Card card);

	@Mapping(target = "seriesName", source = "series.name")
	CardListingResponse toListingResponse(Card card);

	// series, ảnh, giá thị trường và trạng thái do Service quản lý, không map từ request
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "series", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "imagePublicId", ignore = true)
	@Mapping(target = "marketPrice", ignore = true)
	@Mapping(target = "active", ignore = true)
	Card toEntity(CardRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "series", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "imagePublicId", ignore = true)
	@Mapping(target = "marketPrice", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntity(CardRequest request, @MappingTarget Card card);
}