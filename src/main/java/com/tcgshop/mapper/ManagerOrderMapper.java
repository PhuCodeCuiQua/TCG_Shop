package com.tcgshop.mapper;

import com.tcgshop.dto.response.ManagerOrderDetailResponse;
import com.tcgshop.dto.response.ManagerOrderSummaryResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.util.OrderStatusUi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = OrderStatusUi.class)
public interface ManagerOrderMapper {
	@Mapping(target = "shopName", source = "shop.name")
	@Mapping(target = "customerName", expression = "java(order.getUser() == null ? order.getReceiverName() : order.getUser().getFullName())")
	@Mapping(target = "statusLabel", expression = "java(order.getStatus() == null ? \"Không xác định\" : order.getStatus().getDisplayName())")
	@Mapping(target = "statusColor", expression = "java(order.getStatus() == null ? \"secondary\" : OrderStatusUi.color(order.getStatus()))")
	ManagerOrderSummaryResponse toSummary(Order order);

	@Mapping(target = "imageUrl", source = "productImage")
	@Mapping(target = "condition", expression = "java(item.getCardCondition() == null ? null : item.getCardCondition().name())")
	@Mapping(target = "lineTotal", source = "totalPrice")
	ManagerOrderDetailResponse.Item toItem(OrderItem item);
}
