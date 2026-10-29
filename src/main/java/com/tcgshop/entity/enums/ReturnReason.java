package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum ReturnReason {
	DEFECTIVE("Sản phẩm bị lỗi hoặc hư hỏng"),
	NOT_AS_DESCRIBED("Sản phẩm không đúng mô tả"),
	WRONG_ITEM("Giao sai sản phẩm"),
	MISSING_ITEMS("Thiếu sản phẩm"),
	DAMAGED_IN_TRANSIT("Hư hỏng trong quá trình vận chuyển"),
	OTHER("Lý do khác");

	private final String label;

	ReturnReason(String label) {
		this.label = label;
	}
}
