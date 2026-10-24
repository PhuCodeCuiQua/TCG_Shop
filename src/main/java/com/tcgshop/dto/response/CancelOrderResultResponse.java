package com.tcgshop.dto.response;

public record CancelOrderResultResponse(Long orderId, String orderCode, boolean alreadyCancelled) { }
