package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ReturnStatus {

	PENDING("Đang chờ xử lý"),
	APPROVED("Đã duyệt"),
	REJECTED("Đã từ chối"),
	ESCALATED("Đã chuyển cấp quản lý"),
	COMPLETED("Đã hoàn tất");

	private final String displayName;

	ReturnStatus(String displayName) {
		this.displayName = displayName;
	}
}
