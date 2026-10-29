package com.tcgshop.event;

/** Immutable event for notifying the owning shop after a new review is committed. */
public record ReviewCreatedEvent(
		Long shopOwnerId,
		Long productId,
		String productName,
		int rating) {
}
