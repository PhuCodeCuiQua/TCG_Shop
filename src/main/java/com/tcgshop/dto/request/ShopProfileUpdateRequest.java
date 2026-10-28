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
public class ShopProfileUpdateRequest {

	@NotBlank(message = "Vui lòng nhập tên chi nhánh")
	@Size(max = 150, message = "Tên chi nhánh không được vượt quá 150 ký tự")
	private String name;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
	private String phoneNumber;

	@NotBlank(message = "Vui lòng nhập địa chỉ lấy hàng")
	@Size(max = 500, message = "Địa chỉ lấy hàng không được vượt quá 500 ký tự")
	private String pickupAddress;

	@Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
	private String description;
}
