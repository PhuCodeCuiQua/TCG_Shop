package com.tcgshop.service;

import com.tcgshop.dto.response.PublicShopFilterOptionsResponse;
import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PublicShopService {

	PublicShopPageResponse getApprovedShopPage(
			Long shopId,
			ProductType productType,
			String keyword,
			Long gameId,
			Long seriesId,
			Rarity rarity,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable);

	PublicShopFilterOptionsResponse getFilterOptions(Long gameId);

	List<SeriesResponse> getSeriesOptions(Long gameId);
}
