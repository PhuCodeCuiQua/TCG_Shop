package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum RoleName {

	USER("Người dùng / User"),
	VENDOR("Nhà bán hàng / Vendor"),
	MANAGER("Quản lý / Manager"),
	ADMIN("Quản trị viên / Admin"),
	SHIPPER("Nhân viên giao hàng / Shipper");

	private final String displayName;

	RoleName(String displayName) {
		this.displayName = displayName;
	}
}
