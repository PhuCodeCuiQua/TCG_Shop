package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotAskRequest(
		@NotBlank(message = "Vui lòng nhập câu hỏi")
		@Size(max = 500, message = "Câu hỏi không được vượt quá 500 ký tự")
		String question) {
}
