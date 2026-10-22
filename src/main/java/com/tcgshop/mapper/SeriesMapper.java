package com.tcgshop.mapper;

import com.tcgshop.dto.request.SeriesRequest;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.Series;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SeriesMapper {

	// Tên khác nhau nên khai báo @Mapping (mục 6)
	@Mapping(target = "gameId", source = "game.id")
	@Mapping(target = "gameName", source = "game.name")
	SeriesResponse toResponse(Series series);

	// game do Service gán sau khi kiểm tra tồn tại, không map trực tiếp từ gameId
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "game", ignore = true)
	@Mapping(target = "active", ignore = true)
	Series toEntity(SeriesRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "game", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntity(SeriesRequest request, @MappingTarget Series series);
}