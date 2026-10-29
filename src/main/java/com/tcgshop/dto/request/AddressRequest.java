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
public class AddressRequest {

	@NotBlank(message = "Vui lòng nhập tên người nhận")
	@Size(min = 2, max = 100, message = "Tên người nhận phải có từ 2 đến 100 ký tự")
	@Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M} .'-]*$",
			message = "Tên người nhận chỉ gồm chữ cái, khoảng trắng, dấu chấm, nháy đơn hoặc gạch nối và phải bắt đầu bằng chữ cái")
	private String receiverName;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^(0|\\+84)\\d{9}$",
			message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
	private String phoneNumber;

	@NotBlank(message = "Vui lòng nhập tỉnh/thành phố")
	@Size(max = 100, message = "Tỉnh/thành phố không được vượt quá 100 ký tự")
	private String province;

	@NotBlank(message = "Vui lòng nhập quận/huyện")
	@Size(max = 100, message = "Quận/huyện không được vượt quá 100 ký tự")
	private String district;

	@NotBlank(message = "Vui lòng nhập phường/xã")
	@Size(max = 100, message = "Phường/xã không được vượt quá 100 ký tự")
	private String ward;

	@NotBlank(message = "Vui lòng nhập địa chỉ chi tiết")
	@Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
	private String detailAddress;

	private boolean makeDefault;
}
