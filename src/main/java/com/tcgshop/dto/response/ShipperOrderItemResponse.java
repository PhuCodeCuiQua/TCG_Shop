package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ShipperOrderItemResponse(
		String productName,
		String imageUrl,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal totalPrice) { }
