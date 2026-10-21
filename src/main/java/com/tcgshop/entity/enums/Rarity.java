package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum Rarity {

	COMMON("Phổ thông"),
	UNCOMMON("Không phổ biến"),
	RARE("Hiếm"),
	HOLO_RARE("Hiếm lấp lánh"),
	ULTRA_RARE("Cực hiếm"),
	SECRET_RARE("Hiếm bí mật"),
	PROMO("Khuyến mãi");

	private final String displayName;

	Rarity(String displayName) {
		this.displayName = displayName;
	}
}
