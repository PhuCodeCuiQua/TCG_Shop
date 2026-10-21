package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum MediaType {

	IMAGE("Hình ảnh"),
	VIDEO("Video");

	private final String displayName;

	MediaType(String displayName) {
		this.displayName = displayName;
	}
}
