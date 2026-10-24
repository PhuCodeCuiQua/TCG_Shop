package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đánh dấu hoàn tiền; ghi chú bắt buộc (hoàn thủ công hoặc ghi nhận hoàn qua cổng thanh toán). */
public record ReturnRefundRequest(
		@NotBlank(message = "Vui lòng nhập ghi chú hoàn tiền")
		@Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
		String note) {
}