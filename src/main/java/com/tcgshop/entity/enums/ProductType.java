package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ProductType {

	SINGLE("Thẻ đơn / Single"),
	BOOSTER_BOX("Hộp tăng cường / Booster box"),
	BOOSTER_PACK("Gói tăng cường / Booster pack"),
	BUNDLE("Bộ sản phẩm / Bundle"),
	SUPPLY("Phụ kiện / Supply");

	private final String displayName;

	ProductType(String displayName) {
		this.displayName = displayName;
	}
}
