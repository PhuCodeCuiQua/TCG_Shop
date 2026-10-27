package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum NotificationType {

	ORDER_NEW("Đơn hàng mới"),
	ORDER_STATUS("Cập nhật trạng thái đơn hàng"),
	ORDER_ASSIGNED("Đơn hàng được phân công"),
	REVIEW_NEW("Đánh giá mới"),
	REVIEW_REPLY("Phản hồi đánh giá"),
	PRICE_UPDATE("Cập nhật giá"),
	PRICE_ALERT("Cảnh báo giá"),
	SHOP_APPROVAL("Cập nhật duyệt cửa hàng"),
	SYSTEM("Thông báo hệ thống");

	private final String displayName;

	NotificationType(String displayName) {
		this.displayName = displayName;
	}
}
