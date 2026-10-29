package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {

	ACTIVE("Đang bán"),
	INACTIVE("Ngừng bán"),
	OUT_OF_STOCK("Hết hàng"),
	BANNED("Bị cấm");

	private final String displayName;

	ProductStatus(String displayName) {
		this.displayName = displayName;
	}
}
