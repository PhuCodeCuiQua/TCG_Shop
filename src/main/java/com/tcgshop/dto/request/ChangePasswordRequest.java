package com.tcgshop.dto.request;

import com.tcgshop.validation.FieldMatch;
import com.tcgshop.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldMatch(first = "newPassword", second = "confirmPassword", message = "Mật khẩu xác nhận không khớp")
public class ChangePasswordRequest {
	@NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
	private String currentPassword;

	@NotBlank(message = "Vui lòng nhập mật khẩu mới")
	@StrongPassword
	private String newPassword;

	@NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
	private String confirmPassword;
}
