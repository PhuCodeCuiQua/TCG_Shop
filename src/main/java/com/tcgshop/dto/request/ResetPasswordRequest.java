package com.tcgshop.dto.request;

import com.tcgshop.validation.FieldMatch;
import com.tcgshop.validation.StrongPassword;
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
@FieldMatch(first = "newPassword", second = "confirmPassword",
		message = "Mật khẩu xác nhận không khớp")
public class ResetPasswordRequest {

	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 255, message = "Email không được vượt quá 255 ký tự")
	private String email;

	@NotBlank(message = "Vui lòng nhập mã OTP")
	@Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số")
	private String otp;

	@NotBlank(message = "Vui lòng nhập mật khẩu mới")
	@StrongPassword
	private String newPassword;

	@NotBlank(message = "Vui lòng xác nhận mật khẩu")
	private String confirmPassword;
}
