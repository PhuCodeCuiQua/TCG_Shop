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
public class StaffAccountRequest {

	@NotBlank(message = "Vui lòng nhập họ và tên")
	@Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 255, message = "Email tối đa 255 ký tự")
	private String email;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^(0|\\+84)\\d{9}$",
			message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
	private String phoneNumber;
}
