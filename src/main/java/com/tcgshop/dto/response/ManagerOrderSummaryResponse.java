package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ManagerOrderSummaryResponse(Long id, String orderCode, LocalDateTime createdAt,
		String shopName, String customerName, BigDecimal totalAmount, OrderStatus status,
		String statusLabel, String statusColor) { }
