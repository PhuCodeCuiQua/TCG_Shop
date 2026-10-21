package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ShopStatus {

	PENDING("Đang chờ duyệt"),
	APPROVED("Đã duyệt"),
	REJECTED("Bị từ chối"),
	SUSPENDED("Tạm ngưng");

	private final String displayName;

	ShopStatus(String displayName) {
		this.displayName = displayName;
	}
}
