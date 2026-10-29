package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {

	PENDING("Đang xử lý"),
	SUCCESS("Thành công"),
	FAILED("Thất bại");

	private final String displayName;

	TransactionStatus(String displayName) {
		this.displayName = displayName;
	}
}
