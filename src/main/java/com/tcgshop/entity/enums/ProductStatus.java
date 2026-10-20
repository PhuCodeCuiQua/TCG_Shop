package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {

	ACTIVE("Đang bán / Active"),
	HIDDEN("Đã ẩn / Hidden"),
	BANNED("Bị cấm / Banned");

	private final String displayName;

	ProductStatus(String displayName) {
		this.displayName = displayName;
	}
}
