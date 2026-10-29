package com.tcgshop.entity.enums;

import java.util.Set;
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

	public static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
			DELIVERED, RETURN_REQUESTED, RETURN_REJECTED);

	/**
	 * Trạng thái được tính vào sold_count. sold_count chỉ bị trừ khi đơn chuyển sang REFUNDED;
	 * các trạng thái trả hàng trước đó vẫn được tính là đã bán.
	 */
	public static final Set<OrderStatus> SOLD_COUNT_STATUSES = Set.of(
			DELIVERED, RETURN_REQUESTED, RETURN_REJECTED, RETURN_APPROVED, RETURNED);

	/**
	 * Trạng thái đã chốt hoa hồng tại DELIVERED. Snapshot hoa hồng được giữ nguyên qua các trạng thái trả hàng,
	 * kể cả REFUNDED.
	 */
	public static final Set<OrderStatus> COMMISSION_FINALIZED_STATUSES = Set.of(
			DELIVERED, RETURN_REQUESTED, RETURN_REJECTED, RETURN_APPROVED, RETURNED, REFUNDED);

	private final String displayName;

	OrderStatus(String displayName) {
		this.displayName = displayName;
	}

	public boolean countsAsRevenue() {
		return REVENUE_STATUSES.contains(this);
	}
}
