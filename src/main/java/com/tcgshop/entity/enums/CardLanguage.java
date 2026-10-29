package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum CardLanguage {

	ENGLISH("Tiếng Anh"),
	JAPANESE("Tiếng Nhật"),
	OTHER("Ngôn ngữ khác");

	private final String displayName;

	CardLanguage(String displayName) {
		this.displayName = displayName;
	}
}
