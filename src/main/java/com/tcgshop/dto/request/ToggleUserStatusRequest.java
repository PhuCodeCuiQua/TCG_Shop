package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ToggleUserStatusRequest(
        @NotNull(message = "Vui lòng cung cấp trạng thái hiện tại của tài khoản") UserStatus expectedStatus,
        @Size(max = 1000, message = "Lý do khóa tối đa 1000 ký tự") String reason) {
}
