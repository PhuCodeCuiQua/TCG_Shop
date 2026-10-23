package com.tcgshop.service;

import com.tcgshop.dto.response.VendorProductResponse;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorProductService {

	Page<VendorProductResponse> searchCurrentVendorProducts(
			String keyword, ProductType type, ProductStatus status, Pageable pageable);
}
