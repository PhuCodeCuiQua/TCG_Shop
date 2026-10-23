package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

	@NotBlank(message = "Vui lòng nhập họ và tên")
	@Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự")
	@Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M} .'-]*$",
			message = "Họ và tên chỉ gồm chữ cái, khoảng trắng, dấu chấm, nháy đơn hoặc gạch nối và phải bắt đầu bằng chữ cái")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^(0|\\+84)\\d{9}$",
			message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
	private String phoneNumber;
}
