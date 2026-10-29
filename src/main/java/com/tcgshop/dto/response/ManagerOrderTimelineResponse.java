package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.time.LocalDateTime;

public record ManagerOrderTimelineResponse(OrderStatus previousStatus, OrderStatus newStatus,
		String note, LocalDateTime createdAt, String actorName) { }
