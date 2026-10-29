package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.time.LocalDateTime;

public record VendorOrderTimelineResponse(
		OrderStatus status,
		String statusLabel,
		String statusColor,
		LocalDateTime occurredAt,
		String note) { }
