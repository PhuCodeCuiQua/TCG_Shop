package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorOrderItemResponse;
import com.tcgshop.dto.response.VendorOrderSummaryResponse;
import com.tcgshop.dto.response.VendorOrderTimelineResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.repository.TimelineRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorOrderMapper {
	@Mapping(target = "id", source = "id")
	@Mapping(target = "customerName", source = "user.fullName")
	@Mapping(target = "statusLabel", expression = "java(order.getStatus().getDisplayName())")
	@Mapping(target = "statusColor", expression = "java(com.tcgshop.util.OrderStatusUi.color(order.getStatus()))")
	VendorOrderSummaryResponse toSummary(Order order);

	@Mapping(target = "imageUrl", source = "productImage")
	@Mapping(target = "unitPrice", source = "price")
	VendorOrderItemResponse toItem(OrderItem item);

	@Mapping(target = "status", source = "newStatus")
	@Mapping(target = "statusLabel", expression = "java(row.getNewStatus().getDisplayName())")
	@Mapping(target = "statusColor", expression = "java(com.tcgshop.util.OrderStatusUi.color(row.getNewStatus()))")
	@Mapping(target = "occurredAt", source = "createdAt")
	VendorOrderTimelineResponse toTimeline(TimelineRow row);
}
