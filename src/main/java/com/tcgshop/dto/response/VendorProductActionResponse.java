package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ProductStatus;

public record VendorProductActionResponse(Long id, ProductStatus status, String message) { }
