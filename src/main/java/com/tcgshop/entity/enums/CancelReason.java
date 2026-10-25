package com.tcgshop.entity.enums;

public enum CancelReason {
	CHANGE_ADDRESS("Tôi muốn thay đổi địa chỉ giao hàng"),
	CHANGE_ITEMS("Tôi muốn thay đổi sản phẩm hoặc số lượng"),
	BETTER_PRICE("Tôi tìm được giá tốt hơn"),
	CHANGED_MIND("Tôi không còn nhu cầu mua nữa"),
	OTHER("Lý do khác");

	private final String label;

	CancelReason(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
