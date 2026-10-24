package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Đơn đang CONFIRMED, chờ phân công hoặc đổi shipper. */
public record AssignableOrderResponse(
		Long id,
		String orderCode,
		String shopName,
		String receiverName,
		String receiverPhone,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		Long currentShipperId,
		String currentShipperName) {
}