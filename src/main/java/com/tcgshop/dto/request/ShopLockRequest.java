package com.tcgshop.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShopLockRequest {

	@Size(max = 255, message = "Lý do khóa tối đa 255 ký tự")
	private String reason;
}
