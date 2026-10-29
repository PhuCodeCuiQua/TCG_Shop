package com.tcgshop.service.impl;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PublicShopFilterOptionsResponse;
import com.tcgshop.dto.response.PublicShopPageResponse;
import com.tcgshop.dto.response.PublicShopProductResponse;
import com.tcgshop.dto.response.PublicShopResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PublicShopMapper;
import com.tcgshop.mapper.PublicShopProductMapper;
import com.tcgshop.repository.PublicShopProductRepository;
import com.tcgshop.repository.ReviewAggregateRepository;
import com.tcgshop.repository.ReviewRatingAggregate;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PublicShopService;
import com.tcgshop.service.PromotionService;
import com.tcgshop.service.SeriesCatalogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicShopServiceImpl implements PublicShopService {
	private static final String SHOP_NOT_FOUND_MESSAGE = "Chi nhánh không tồn tại hoặc đã bị khóa";

	private final ShopRepository shopRepository;
	private final PublicShopProductRepository publicShopProductRepository;
	private final PublicShopMapper publicShopMapper;
	private final PublicShopProductMapper publicShopProductMapper;
	private final ReviewAggregateRepository reviewAggregateRepository;
	private final PromotionService promotionService;
	private final GameCatalogService gameCatalogService;
	private final SeriesCatalogService seriesCatalogService;

	@Override
	@Transactional(readOnly = true)
	public PublicShopPageResponse getApprovedShopPage(
			Long shopId,
			ProductType productType,
			String keyword,
			Long gameId,
			Long seriesId,
			Rarity rarity,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable) {
		validateFilters(gameId, seriesId, minPrice, maxPrice);
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
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
		Page<PublicShopProductResponse> products = publicShopProductRepository.findPublicProducts(
				shop.getId(), ShopStatus.APPROVED, ProductStatus.ACTIVE,
				productType, normalizedKeyword, gameId, seriesId, rarity, minPrice, maxPrice,
				ProductType.SINGLE, pageable).map(publicShopProductMapper::toResponse);
		Map<Long, BigDecimal> effectivePrices = promotionService.getEffectivePrices(
				products.getContent().stream().map(PublicShopProductResponse::id).toList());
		products = products.map(product -> new PublicShopProductResponse(
				product.id(), product.name(), product.description(), product.price(), product.price(),
				effectivePrices.getOrDefault(product.id(), product.price()), product.stock(),
				product.type(), product.imageUrl()));
		return new PublicShopPageResponse(shopResponse, products);
	}

	@Override
	@Transactional(readOnly = true)
	public PublicShopFilterOptionsResponse getFilterOptions(Long gameId) {
		validateGameId(gameId);
		PageRequest optionsPage = PageRequest.of(0, 1000, Sort.by("name").ascending());
		List<GameResponse> games = gameCatalogService.search(null, true, optionsPage).content();
		List<SeriesResponse> series = getSeriesOptions(gameId);
		return new PublicShopFilterOptionsResponse(games, series);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SeriesResponse> getSeriesOptions(Long gameId) {
		validateGameId(gameId);
		PageRequest optionsPage = PageRequest.of(0, 1000, Sort.by("name").ascending());
		return seriesCatalogService.search(null, gameId, true, optionsPage).content();
	}

	private void validateFilters(Long gameId, Long seriesId, BigDecimal minPrice, BigDecimal maxPrice) {
		validateGameId(gameId);
		if (seriesId != null && seriesId <= 0) {
			throw new BadRequestException("Series không hợp lệ");
		}
		if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
			throw new BadRequestException("Khoảng giá không được âm");
		}
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BadRequestException("Giá từ phải nhỏ hơn hoặc bằng giá đến");
		}
	}

	private void validateGameId(Long gameId) {
		if (gameId != null && gameId <= 0) {
			throw new BadRequestException("Game không hợp lệ");
		}
	}
}
