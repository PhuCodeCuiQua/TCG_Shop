package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GameRequest(
		@NotBlank(message = "Tên game không được để trống")
		@Size(max = 100, message = "Tên game tối đa 100 ký tự")
		String name,

		@NotBlank(message = "Mã game không được để trống")
		@Size(max = 30, message = "Mã game tối đa 30 ký tự")
		@Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã game chỉ gồm chữ HOA, số và dấu gạch dưới")
		String code,

		@Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
		String description) {
}