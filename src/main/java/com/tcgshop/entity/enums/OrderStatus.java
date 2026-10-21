package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

	NEW("Mới"),
	CONFIRMED("Đã xác nhận"),
	PICKED_UP("Đã lấy hàng"),
	SHIPPING("Đang giao"),
	DELIVERED("Đã giao"),
	DELIVERY_FAILED("Giao hàng thất bại"),
	CANCELLED("Đã hủy"),
	RETURN_REQUESTED("Đã yêu cầu trả hàng"),
	RETURN_APPROVED("Đã duyệt trả hàng"),
	RETURN_REJECTED("Đã từ chối trả hàng"),
	RETURNED("Đã trả hàng"),
	REFUNDED("Đã hoàn tiền");

	private final String displayName;

	OrderStatus(String displayName) {
		this.displayName = displayName;
	}
}
