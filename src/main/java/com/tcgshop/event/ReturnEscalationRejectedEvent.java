package com.tcgshop.event;

public record ReturnEscalationRejectedEvent(Long orderId, String orderCode, Long buyerId, String note) {
}
