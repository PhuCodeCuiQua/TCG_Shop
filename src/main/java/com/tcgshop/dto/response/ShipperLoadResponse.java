package com.tcgshop.dto.response;

/** Shipper đang hoạt động kèm số đơn đang giao, để Manager chọn người ít việc. */
public record ShipperLoadResponse(
		Long id,
		String fullName,
		String email,
		Long shippingCount) {
}