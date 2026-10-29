package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorReviewReplyRequest(
		@NotBlank(message = "Nội dung phản hồi không được để trống")
		@Size(max = 2000, message = "Phản hồi tối đa 2.000 ký tự")
		String reply) {
}
