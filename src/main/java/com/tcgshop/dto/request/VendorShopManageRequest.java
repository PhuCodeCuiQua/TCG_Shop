package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class VendorShopManageRequest {

	private MultipartFile logo;

	private MultipartFile banner;

	@Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
	private String description;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
	private String phoneNumber;

	@NotBlank(message = "Vui lòng nhập địa chỉ lấy hàng")
	@Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
	private String pickupAddress;
}
