package com.tcgshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcgshop.validation.MinCommentLength;

public record ReviewSubmitRequest(
		@NotNull(message = "Vui lòng chọn số sao")
		@Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao")
		@Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao")
		Integer rating,
		@Size(max = 2000, message = "Nhận xét không được vượt quá 2.000 ký tự")
		@MinCommentLength
		String comment) {
}
