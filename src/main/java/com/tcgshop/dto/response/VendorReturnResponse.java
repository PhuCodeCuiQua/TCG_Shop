package com.tcgshop.dto.response;

import java.time.LocalDateTime;

public record VendorReturnResponse(
		Long orderId,
		String orderCode,
		String customerName,
		String reason,
		String proofImageUrl,
		LocalDateTime requestedAt) {
}
