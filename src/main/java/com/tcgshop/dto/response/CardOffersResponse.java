package com.tcgshop.dto.response;

import java.util.List;

/** availableConditions: mọi tình trạng đang có hàng (trước khi lọc), dùng cho ô lọc. */
public record CardOffersResponse(List<CardOfferResponse> offers, List<String> availableConditions) {
}