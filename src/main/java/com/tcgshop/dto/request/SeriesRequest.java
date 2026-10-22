package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dữ liệu tạo/sửa Series. gameId chỉ dùng khi tạo mới; sửa không cho đổi Game. */
public record SeriesRequest(
		@NotNull(message = "Vui lòng chọn game")
		Long gameId,

		@NotBlank(message = "Tên series không được để trống")
		@Size(max = 150, message = "Tên series tối đa 150 ký tự")
		String name,

		@NotBlank(message = "Mã series không được để trống")
		@Size(max = 30, message = "Mã series tối đa 30 ký tự")
		@Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã series chỉ gồm chữ HOA, số và dấu gạch dưới")
		String code,

		LocalDate releaseDate) {
}