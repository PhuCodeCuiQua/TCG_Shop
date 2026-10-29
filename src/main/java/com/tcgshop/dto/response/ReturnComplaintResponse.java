package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReturnComplaintResponse(
		Long orderId,
		String orderCode,
		String buyerName,
		String shopName,
		BigDecimal totalAmount,
		String reason,
		OrderStatus orderStatus,
		ReturnStatus returnStatus,
		LocalDateTime requestedAt) {
}