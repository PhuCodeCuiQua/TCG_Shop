package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum CardCondition {

	MINT("Như mới / Mint"),
	NEAR_MINT("Gần như mới / Near mint"),
	LIGHTLY_PLAYED("Ít trầy xước / Lightly played"),
	MODERATELY_PLAYED("Trầy xước vừa / Moderately played"),
	HEAVILY_PLAYED("Trầy xước nhiều / Heavily played"),
	DAMAGED("Hư hỏng / Damaged");

	private final String displayName;

	CardCondition(String displayName) {
		this.displayName = displayName;
	}
}
