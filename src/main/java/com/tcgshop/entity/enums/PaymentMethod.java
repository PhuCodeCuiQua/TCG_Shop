package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {

	COD("Thanh toán khi nhận hàng"),
	MOMO("MoMo");

	private final String displayName;

	PaymentMethod(String displayName) {
		this.displayName = displayName;
	}
}
