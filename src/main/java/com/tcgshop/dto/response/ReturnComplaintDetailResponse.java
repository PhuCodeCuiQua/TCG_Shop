package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReturnComplaintDetailResponse(
		Long orderId,
		String orderCode,
		String buyerName,
		String shopName,
		BigDecimal totalAmount,
		String reason,
		String proofImageUrl,
		String proofVideoUrl,
		String vendorNote,
		String adminNote,
		OrderStatus orderStatus,
		ReturnStatus returnStatus,
		LocalDateTime requestedAt,
		LocalDateTime decidedAt,
		List<TimelineItem> timeline) {

	public record TimelineItem(OrderStatus previousStatus, OrderStatus newStatus, String note,
			LocalDateTime createdAt, String actorName) {
	}
}