package com.tcgshop.event;

public record ReturnRequestEscalatedEvent(Long orderId, String orderCode, Long buyerId, Long shopId,
		Long shopOwnerId) {
}
