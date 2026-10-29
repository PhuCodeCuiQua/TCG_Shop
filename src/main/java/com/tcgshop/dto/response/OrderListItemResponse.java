package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderListItemResponse(Long orderId, String orderCode, String groupCode, Long shopId,
		String shopName, OrderStatus status, String statusLabel, String statusColor, LocalDateTime createdAt,
		BigDecimal totalAmount, int itemCount, List<OrderItemPreviewResponse> previewItems, int moreItemsCount) {
	public OrderListItemResponse {
		previewItems = previewItems == null ? List.of() : List.copyOf(previewItems);
	}
}
