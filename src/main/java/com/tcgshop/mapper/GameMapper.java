package com.tcgshop.mapper;

import com.tcgshop.dto.request.GameRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.entity.Game;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GameMapper {

	GameResponse toResponse(Game game);

	// Các trường hệ thống và logo do Service quản lý, không map từ request
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "logoUrl", ignore = true)
	@Mapping(target = "logoPublicId", ignore = true)
	@Mapping(target = "active", ignore = true)
	Game toEntity(GameRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "logoUrl", ignore = true)
	@Mapping(target = "logoPublicId", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntity(GameRequest request, @MappingTarget Game game);
}