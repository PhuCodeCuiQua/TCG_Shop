package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EscalateReturnRequest {
	@NotBlank
	@Size(min = 10, max = 500, message = "Mô tả chi tiết từ 10 đến 500 ký tự")
	private String detail;
}
