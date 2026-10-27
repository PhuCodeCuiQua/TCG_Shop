package com.tcgshop.mapper;

import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserManagementMapper {

	UpdateProfileRequest toEditRequest(UserManagementResponse user);

	@Mapping(target = "roles", source = "roles", qualifiedByName = "roleNames")
	@Mapping(target = "statusDisplayName", expression = "java(user.getStatus().getDisplayName())")
	UserManagementResponse toResponse(User user);

	@Named("roleNames")
	default List<RoleName> roleNames(Set<Role> roles) {
		List<RoleName> roleNames = new ArrayList<>(roles.size());
		for (Role role : roles) {
			if (role != null && role.getName() != null) {
				roleNames.add(role.getName());
			}
		}
		roleNames.sort(null);
		return List.copyOf(roleNames);
	}
}
