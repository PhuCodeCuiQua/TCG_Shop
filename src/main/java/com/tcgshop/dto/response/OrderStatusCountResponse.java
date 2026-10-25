package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;

public record OrderStatusCountResponse(OrderStatus status, String label, long count) {
}