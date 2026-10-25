package com.tcgshop.service;

import com.tcgshop.dto.response.ProductDetailResponse;

public interface ProductDetailService {
	/** Sản phẩm không tồn tại, bị gỡ hoặc shop không còn APPROVED thì ném ResourceNotFoundException. */
	ProductDetailResponse getDetail(Long productId);
}