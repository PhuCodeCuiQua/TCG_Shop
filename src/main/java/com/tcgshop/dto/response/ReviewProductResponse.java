package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ReviewProductResponse(Long id, String name, BigDecimal averageRating, int reviewCount) {
}
