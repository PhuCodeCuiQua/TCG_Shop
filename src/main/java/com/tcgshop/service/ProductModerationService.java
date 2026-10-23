package com.tcgshop.service;

import com.tcgshop.dto.request.ProductBanRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.dto.response.ShopOptionResponse;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductModerationService {

	PageResponse<ProductModerationResponse> search(
			Long shopId, ProductType type, ProductStatus status, String keyword, Pageable pageable);

	List<ShopOptionResponse> getShopOptions();

	ProductModerationResponse getById(Long productId);

	ProductModerationResponse ban(Long productId, ProductBanRequest request);

	ProductModerationResponse restore(Long productId);
}
