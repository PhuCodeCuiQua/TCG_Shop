package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum OtpPurpose {

	REGISTER("Đăng ký"),
	FORGOT_PASSWORD("Quên mật khẩu");

	private final String displayName;

	OtpPurpose(String displayName) {
		this.displayName = displayName;
	}
}
