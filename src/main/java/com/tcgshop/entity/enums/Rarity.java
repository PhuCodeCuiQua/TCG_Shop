package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum Rarity {

	COMMON("Phổ thông / Common"),
	UNCOMMON("Không phổ biến / Uncommon"),
	RARE("Hiếm / Rare"),
	HOLO_RARE("Hiếm lấp lánh / Holo rare"),
	ULTRA_RARE("Cực hiếm / Ultra rare"),
	SECRET_RARE("Hiếm bí mật / Secret rare"),
	PROMO("Khuyến mãi / Promo");

	private final String displayName;

	Rarity(String displayName) {
		this.displayName = displayName;
	}
}
