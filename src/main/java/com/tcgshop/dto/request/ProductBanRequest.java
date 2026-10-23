package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductBanRequest {

	@NotBlank(message = "Vui lòng nhập lý do gỡ sản phẩm")
	@Size(max = 255, message = "Lý do gỡ sản phẩm tối đa 255 ký tự")
	private String reason;
}
