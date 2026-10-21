package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum PromotionType {

	PRODUCT_DISCOUNT("Giảm giá sản phẩm"),
	SHIPPING_DISCOUNT("Giảm phí vận chuyển");

	private final String displayName;

	PromotionType(String displayName) {
		this.displayName = displayName;
	}
}
