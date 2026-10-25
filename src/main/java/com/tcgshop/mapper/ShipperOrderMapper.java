package com.tcgshop.mapper;

import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderItemResponse;
import com.tcgshop.dto.response.ShipperOrderSummaryResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.enums.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipperOrderMapper {
	@Mapping(target = "codAmount", expression = "java(codAmount(order))")
	@Mapping(target = "statusLabel", expression = "java(order.getStatus().getDisplayName())")
	@Mapping(target = "statusColor", expression = "java(com.tcgshop.util.OrderStatusUi.color(order.getStatus()))")
	ShipperOrderSummaryResponse toSummary(Order order);

	@Mapping(target = "shopName", source = "shop.name")
	@Mapping(target = "pickupAddress", source = "shop.pickupAddress")
	@Mapping(target = "shopPhone", source = "shop.phoneNumber")
	@Mapping(target = "pickupNote", source = "note")
	@Mapping(target = "recipientName", source = "receiverName")
	@Mapping(target = "recipientPhone", source = "receiverPhone")
	@Mapping(target = "codAmount", expression = "java(codAmount(order))")
	@Mapping(target = "statusLabel", expression = "java(order.getStatus().getDisplayName())")
	@Mapping(target = "statusColor", expression = "java(com.tcgshop.util.OrderStatusUi.color(order.getStatus()))")
	ShipperOrderDetailResponse toDetail(Order order);

	@Mapping(target = "imageUrl", source = "productImage")
	@Mapping(target = "unitPrice", source = "price")
	ShipperOrderItemResponse toItem(OrderItem item);

	default java.math.BigDecimal codAmount(Order order) {
		if (order.getOrderGroup() == null
				|| order.getOrderGroup().getPaymentMethod() != PaymentMethod.COD
				|| order.isCodCollected()) {
			return null;
		}
		return order.getTotalAmount();
	}
}
