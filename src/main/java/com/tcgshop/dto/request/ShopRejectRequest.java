package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lý do từ chối hồ sơ shop; bắt buộc nhập. */
public record ShopRejectRequest(
		@NotBlank(message = "Vui lòng nhập lý do từ chối")
		@Size(max = 255, message = "Lý do tối đa 255 ký tự")
		String reason) {
}