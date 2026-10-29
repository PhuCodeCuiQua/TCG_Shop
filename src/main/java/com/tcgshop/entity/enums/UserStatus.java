package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

	ACTIVE("Đang hoạt động"),
	INACTIVE("Chưa kích hoạt"),
	BLOCKED("Bị khóa");

	private final String displayName;

	UserStatus(String displayName) {
		this.displayName = displayName;
	}
}
