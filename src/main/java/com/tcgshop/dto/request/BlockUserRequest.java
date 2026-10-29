package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockUserRequest {

	@NotBlank(message = "Vui lòng nhập lý do khóa tài khoản")
	@Size(max = 1000, message = "Lý do khóa tối đa 1000 ký tự")
	private String reason;
}
