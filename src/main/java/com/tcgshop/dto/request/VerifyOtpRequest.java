package com.tcgshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerifyOtpRequest {
	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 255, message = "Email không được vượt quá 255 ký tự")
	private String email;

	@NotBlank(message = "Vui lòng nhập mã OTP")
	@Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
	private String otp;
}
