package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum CardLanguage {

	ENGLISH("Tiếng Anh / English"),
	JAPANESE("Tiếng Nhật / Japanese"),
	OTHER("Ngôn ngữ khác / Other");

	private final String displayName;

	CardLanguage(String displayName) {
		this.displayName = displayName;
	}
}
