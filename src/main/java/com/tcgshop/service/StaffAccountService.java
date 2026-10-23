package com.tcgshop.service;

import com.tcgshop.dto.request.StaffAccountRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.StaffAccountResponse;
import com.tcgshop.entity.enums.UserStatus;
import org.springframework.data.domain.Pageable;

public interface StaffAccountService {

	StaffAccountResponse createManager(StaffAccountRequest request);

	StaffAccountResponse createShipper(StaffAccountRequest request);

	PageResponse<StaffAccountResponse> searchShippers(String keyword, UserStatus status, Pageable pageable);
}
