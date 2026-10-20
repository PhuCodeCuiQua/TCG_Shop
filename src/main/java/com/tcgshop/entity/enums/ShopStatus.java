package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ShopStatus {

	PENDING("Đang chờ duyệt / Pending"),
	APPROVED("Đã duyệt / Approved"),
	REJECTED("Bị từ chối / Rejected"),
	LOCKED("Đã khóa / Locked");

	private final String displayName;

	ShopStatus(String displayName) {
		this.displayName = displayName;
	}
}
