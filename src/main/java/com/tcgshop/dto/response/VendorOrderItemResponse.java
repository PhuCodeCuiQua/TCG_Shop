package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record VendorOrderItemResponse(
		String productName,
		String imageUrl,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal totalPrice) { }
