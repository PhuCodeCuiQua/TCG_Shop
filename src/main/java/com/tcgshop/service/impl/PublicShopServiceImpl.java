package com.tcgshop.service.impl;

import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.dto.response.PublicShopProductResponse;
import com.tcgshop.dto.response.PublicShopResponse;
import com.tcgshop.repository.ReviewAggregateRepository;
import com.tcgshop.repository.ReviewRatingAggregate;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PublicShopMapper;
import com.tcgshop.mapper.PublicShopProductMapper;
import com.tcgshop.repository.PublicShopProductRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.service.PublicShopService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicShopServiceImpl implements PublicShopService {
	private static final String SHOP_NOT_FOUND_MESSAGE = "Shop không tồn tại hoặc đã bị khóa";

	private final ShopRepository shopRepository;
	private final PublicShopProductRepository publicShopProductRepository;
	private final PublicShopMapper publicShopMapper;
	private final PublicShopProductMapper publicShopProductMapper;
	private final ReviewAggregateRepository reviewAggregateRepository;

	@Override
	@Transactional(readOnly = true)
	public PublicShopPageResponse getApprovedShopPage(Long shopId, ProductType productType, Pageable pageable) {
		Shop shop = shopRepository.findById(shopId)
				.filter(candidate -> candidate.getStatus() == ShopStatus.APPROVED)
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException(SHOP_NOT_FOUND_MESSAGE));

		ReviewRatingAggregate rating = reviewAggregateRepository.summarizeByShopId(shop.getId()).orElse(null);
		int ratingCount = rating == null || rating.getReviewCount() == null
				? 0 : Math.toIntExact(rating.getReviewCount());
		BigDecimal averageRating = ratingCount == 0 || rating.getAverageRating() == null
				? null : BigDecimal.valueOf(rating.getAverageRating()).setScale(2, RoundingMode.HALF_UP);
		PublicShopResponse mappedShop = publicShopMapper.toResponse(shop);
		PublicShopResponse shopResponse = new PublicShopResponse(
				mappedShop.id(), mappedShop.name(), mappedShop.slug(), mappedShop.description(),
				mappedShop.logoUrl(), mappedShop.bannerUrl(), mappedShop.phoneNumber(),
				mappedShop.pickupAddress(), averageRating, ratingCount);
		Page<PublicShopProductResponse> products = publicShopProductRepository.findPublicProducts(
				shop.getId(), ShopStatus.APPROVED, ProductStatus.ACTIVE,
				productType, pageable).map(publicShopProductMapper::toResponse);
		return new PublicShopPageResponse(shopResponse, products);
	}
}
