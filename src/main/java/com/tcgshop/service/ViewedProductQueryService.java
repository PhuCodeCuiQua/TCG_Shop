package com.tcgshop.service;

import com.tcgshop.dto.response.ViewedProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ViewedProductQueryService {
	Page<ViewedProductResponse> getRecentProducts(Long userId, Pageable pageable);
}
