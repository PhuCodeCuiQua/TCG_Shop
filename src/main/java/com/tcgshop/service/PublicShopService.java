package com.tcgshop.service;

import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.entity.enums.ProductType;
import org.springframework.data.domain.Pageable;

public interface PublicShopService {

	PublicShopPageResponse getApprovedShopPage(Long shopId, ProductType productType, Pageable pageable);
}
