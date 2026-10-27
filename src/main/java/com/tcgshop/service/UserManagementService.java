package com.tcgshop.service;

import com.tcgshop.dto.request.BlockUserRequest;
import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.dto.request.ToggleUserStatusRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {

	PageResponse<UserManagementResponse> search(
			String keyword, RoleName roleName, UserStatus status, Pageable pageable);

	UserManagementResponse getById(Long userId);

	UserManagementResponse block(Long userId, BlockUserRequest request);

	UserManagementResponse unblock(Long userId);

	UserManagementResponse update(Long userId, UpdateProfileRequest request);

	UserManagementResponse toggleStatus(Long userId, ToggleUserStatusRequest request);
}
