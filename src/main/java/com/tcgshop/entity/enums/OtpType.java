package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum OtpType {

	REGISTER("Đăng ký / Register"),
	RESET_PASSWORD("Đặt lại mật khẩu / Reset password");

	private final String displayName;

	OtpType(String displayName) {
		this.displayName = displayName;
	}
}
