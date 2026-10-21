package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum CardCondition {

	MINT("Như mới", "M"),
	NEAR_MINT("Gần như mới", "NM"),
	LIGHTLY_PLAYED("Đã qua sử dụng nhẹ", "LP"),
	MODERATELY_PLAYED("Đã qua sử dụng vừa", "MP"),
	HEAVILY_PLAYED("Đã qua sử dụng nhiều", "HP"),
	DAMAGED("Hư hỏng", "DMG");

	private final String displayName;
	private final String abbreviation;

	CardCondition(String displayName, String abbreviation) {
		this.displayName = displayName;
		this.abbreviation = abbreviation;
	}
}
