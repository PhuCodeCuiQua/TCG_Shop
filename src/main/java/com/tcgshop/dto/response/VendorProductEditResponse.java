package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

public record VendorProductEditResponse(Long id, String name, ProductType type, BigDecimal price,
		Integer stock, String description, String imageUrl) { }
