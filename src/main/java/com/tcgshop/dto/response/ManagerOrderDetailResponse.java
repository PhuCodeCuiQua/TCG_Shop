package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerOrderDetailResponse(Long id, String orderCode, LocalDateTime createdAt,
		OrderStatus status, String statusLabel, String statusColor, String shopName,
		String customerName, String customerEmail, String receiverName, String receiverPhone,
		String shippingAddress, String note, String paymentMethod, String paymentStatus,
		BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal totalAmount,
		List<Item> items, List<ManagerOrderTimelineResponse> timeline) {
	public ManagerOrderDetailResponse {
		items = items == null ? List.of() : List.copyOf(items);
		timeline = timeline == null ? List.of() : List.copyOf(timeline);
	}

	public record Item(String productName, String imageUrl, String condition, BigDecimal price,
			int quantity, BigDecimal lineTotal) { }
}
