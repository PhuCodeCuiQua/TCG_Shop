package com.tcgshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {
	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	private String email;

	@NotBlank(message = "Vui lòng nhập mật khẩu")
	private String password;

	private String returnUrl;
}
