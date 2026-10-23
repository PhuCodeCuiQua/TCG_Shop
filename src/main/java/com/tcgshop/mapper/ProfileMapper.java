package com.tcgshop.mapper;

import com.tcgshop.dto.response.ProfileResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import org.mapstruct.Mapper;

@Mapper(config = CommonMapperConfig.class)
public interface ProfileMapper {

	ProfileResponse toResponse(User user);

	default RoleName map(Role role) {
		return role == null ? null : role.getName();
	}
}
