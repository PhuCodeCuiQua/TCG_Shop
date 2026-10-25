package com.tcgshop.mapper;

import com.tcgshop.dto.response.HomeGameResponse;
import com.tcgshop.dto.response.HomeProductResponse;
import com.tcgshop.entity.Game;
import com.tcgshop.repository.HomeProductProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HomePageMapper {

	@Mapping(target = "mark", expression = "java(mark(game.getName()))")
	HomeGameResponse toGameResponse(Game game);

	HomeProductResponse toProductResponse(HomeProductProjection projection);

	default String mark(String name) {
		return name == null || name.isBlank() ? "?" : name.substring(0, 1);
	}
}
