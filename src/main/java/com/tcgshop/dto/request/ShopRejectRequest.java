package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShopRejectRequest {

	@NotBlank(message = "Vui lòng nhập lý do từ chối")
	@Size(max = 255, message = "Lý do từ chối tối đa 255 ký tự")
	private String reason;
}
