package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum DiscountType {

	FIXED_AMOUNT("Số tiền cố định"),
	PERCENTAGE("Phần trăm");

	private final String displayName;

	DiscountType(String displayName) {
		this.displayName = displayName;
	}
}
