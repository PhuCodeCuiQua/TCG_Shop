package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ActiveStatus {

	ACTIVE("Đang hoạt động"),
	INACTIVE("Ngừng hoạt động");

	private final String displayName;

	ActiveStatus(String displayName) {
		this.displayName = displayName;
	}
}
