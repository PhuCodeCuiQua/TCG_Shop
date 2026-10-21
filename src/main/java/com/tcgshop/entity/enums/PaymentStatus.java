package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {

	PENDING("Chờ thanh toán"),
	PAID("Đã thanh toán"),
	FAILED("Thanh toán thất bại"),
	REFUND_PENDING("Đang chờ hoàn tiền"),
	REFUNDED("Đã hoàn tiền");

	private final String displayName;

	PaymentStatus(String displayName) {
		this.displayName = displayName;
	}
}
