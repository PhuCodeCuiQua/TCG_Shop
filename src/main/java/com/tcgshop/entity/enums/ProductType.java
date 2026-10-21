package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ProductType {

	SINGLE("Thẻ đơn"),
	BOOSTER_BOX("Hộp tăng cường"),
	BOOSTER_PACK("Gói tăng cường"),
	BUNDLE("Bộ sản phẩm"),
	SUPPLY("Phụ kiện");

	private final String displayName;

	ProductType(String displayName) {
		this.displayName = displayName;
	}
}
