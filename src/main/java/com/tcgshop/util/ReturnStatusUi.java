package com.tcgshop.util;

import com.tcgshop.entity.enums.ReturnStatus;

public final class ReturnStatusUi {
	private ReturnStatusUi() { }

	public static String label(ReturnStatus status) {
		if (status == null) return "";
		return switch (status) {
			case PENDING -> "Chờ cửa hàng duyệt";
			case APPROVED -> "Đã duyệt, chờ lấy hàng về";
			case REJECTED -> "Bị từ chối";
			case ESCALATED -> "Đang khiếu nại, chờ quản lý xử lý";
			case COMPLETED -> "Đã hoàn tất";
		};
	}

	public static String color(ReturnStatus status) {
		if (status == null) return "secondary";
		return switch (status) {
			case PENDING, ESCALATED -> "warning";
			case APPROVED -> "info";
			case REJECTED -> "danger";
			case COMPLETED -> "success";
		};
	}
}
