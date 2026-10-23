package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record VendorSingleProductResponse(Long id, String name, BigDecimal price) {
}
