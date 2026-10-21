package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum RoleName {

	ROLE_USER("Người dùng"),
	ROLE_VENDOR("Nhà bán hàng"),
	ROLE_MANAGER("Quản lý"),
	ROLE_ADMIN("Quản trị viên"),
	ROLE_SHIPPER("Nhân viên giao hàng");

	private final String displayName;

	RoleName(String displayName) {
		this.displayName = displayName;
	}
}
