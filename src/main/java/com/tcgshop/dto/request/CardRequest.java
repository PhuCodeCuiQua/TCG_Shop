package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dữ liệu tạo/sửa thẻ gốc. Ảnh xử lý riêng qua CloudinaryService nên không nằm ở đây. */
public record CardRequest(
		@NotNull(message = "Vui lòng chọn series")
		Long seriesId,

		@NotBlank(message = "Tên thẻ không được để trống")
		@Size(max = 200, message = "Tên thẻ tối đa 200 ký tự")
		String name,

		@NotBlank(message = "Số thẻ không được để trống")
		@Size(max = 30, message = "Số thẻ tối đa 30 ký tự")
		String cardNumber,

		@NotNull(message = "Vui lòng chọn độ hiếm")
		Rarity rarity,

		@NotNull(message = "Vui lòng chọn ngôn ngữ")
		CardLanguage language) {
}