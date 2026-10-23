package com.tcgshop.mapper;

import com.tcgshop.dto.response.StaffAccountResponse;
import com.tcgshop.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffAccountMapper {

	@Mapping(target = "statusDisplayName", expression = "java(user.getStatus().getDisplayName())")
	@Mapping(target = "activeDeliveryCount", source = "activeDeliveryCount")
	StaffAccountResponse toResponse(User user, long activeDeliveryCount);
}
