package com.tcgshop.dto.request;

import jakarta.validation.constraints.Size;

/** Lý do khóa shop; không bắt buộc, chỉ dùng trong nội dung thông báo cho chủ shop. */
public record ShopLockRequest(
		@Size(max = 255, message = "Lý do tối đa 255 ký tự")
		String reason) {
}