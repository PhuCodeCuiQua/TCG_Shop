package com.tcgshop.repository;

import java.math.BigDecimal;

public record PublicProductPriceProjection(Long itemId, Long productId, BigDecimal price) {
}
