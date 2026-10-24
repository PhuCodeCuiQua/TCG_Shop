package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Quyết định khiếu nại trả hàng (chấp nhận hoặc từ chối); ghi chú bắt buộc. */
public record ReturnComplaintDecisionRequest(
		@NotBlank(message = "Vui lòng nhập ghi chú quyết định")
		@Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
		String note) {
}