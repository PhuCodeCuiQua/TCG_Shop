package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record SiblingOrderResponse(Long orderId, String orderCode, String shopName,
		String statusLabel, String statusColor, BigDecimal totalAmount) { }
