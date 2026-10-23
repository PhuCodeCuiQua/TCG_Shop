package com.tcgshop.dto.response;

import org.springframework.data.domain.Page;

public record PublicShopPageResponse(
		PublicShopResponse shop,
		Page<PublicShopProductResponse> products) {
}
