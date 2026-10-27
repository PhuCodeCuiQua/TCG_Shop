package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.dto.response.PublicProductSearchOptionsResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.mapper.PublicProductSearchMapper;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ProductThumbnailProjection;
import com.tcgshop.repository.PublicProductSpecifications;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PublicProductSearchService;
import com.tcgshop.service.SeriesCatalogService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProductSearchServiceImpl implements PublicProductSearchService {

	private static final int PAGE_SIZE = 20;
	private static final int FILTER_OPTIONS_PAGE_SIZE = 1000;

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final PublicProductSearchMapper publicProductSearchMapper;
	private final GameCatalogService gameCatalogService;
	private final SeriesCatalogService seriesCatalogService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PublicProductSearchItemResponse> search(PublicProductSearchRequest filters) {
		boolean topTwenty = filters.isTopTwenty();
		PublicProductSearchRequest.SortBy sort = topTwenty
				? filters.getTab().getSortBy()
				: filters.getSort();
		int page = topTwenty ? 0 : filters.getPage();
		Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.unsorted());
		var results = productRepository.searchPublic(
				PublicProductSpecifications.from(filters), pageable, sort, topTwenty);
		List<PublicProductSearchItemResponse> items = results.getContent().stream()
				.map(publicProductSearchMapper::toResponse)
				.toList();
		if (items.isEmpty()) {
			return PageResponse.from(results, publicProductSearchMapper::toResponse);
		}

		Map<Long, String> imageUrls = productImageRepository.findThumbnails(
				items.stream().map(PublicProductSearchItemResponse::productId).toList()).stream()
				.filter(thumbnail -> thumbnail.getImageUrl() != null && !thumbnail.getImageUrl().isBlank())
				.collect(Collectors.toMap(ProductThumbnailProjection::getProductId,
						ProductThumbnailProjection::getImageUrl, (first, ignored) -> first));
		Map<Long, String> seriesFallbacks = productImageRepository.findSeriesFallbacks(
				items.stream().map(PublicProductSearchItemResponse::productId).toList()).stream()
				.filter(thumbnail -> thumbnail.getImageUrl() != null && !thumbnail.getImageUrl().isBlank())
				.collect(Collectors.toMap(ProductThumbnailProjection::getProductId,
						ProductThumbnailProjection::getImageUrl, (first, ignored) -> first));
		List<Long> supplyProductIds = items.stream()
				.filter(item -> item.type() == ProductType.SUPPLY)
				.map(PublicProductSearchItemResponse::productId)
				.toList();
		Map<Long, String> supplyMainImages = supplyProductIds.isEmpty()
				? Map.of()
				: productImageRepository.findMainImages(supplyProductIds).stream()
						.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
						.collect(Collectors.toMap(
								image -> image.getProduct().getId(),
								ProductImage::getImageUrl,
								(first, ignored) -> first));
		List<PublicProductSearchItemResponse> withImages = items.stream()
				.map(item -> withImageUrl(item, imageUrls.get(item.productId()),
						seriesFallbacks.get(item.productId()), supplyMainImages.get(item.productId())))
				.toList();
		return new PageResponse<>(withImages, results.getNumber(), results.getSize(),
				results.getTotalElements(), results.getTotalPages(), results.isFirst(), results.isLast());
	}

	private PublicProductSearchItemResponse withImageUrl(
			PublicProductSearchItemResponse item,
			String productImageUrl,
			String seriesFallbackUrl,
			String supplyMainImageUrl) {
		String imageUrl;
		if (item.type() == ProductType.SUPPLY) {
			imageUrl = supplyMainImageUrl;
		} else {
			imageUrl = productImageUrl == null || productImageUrl.isBlank()
					? item.imageUrl()
					: productImageUrl;
			if (imageUrl == null || imageUrl.isBlank()) {
				imageUrl = seriesFallbackUrl;
			}
		}
		return new PublicProductSearchItemResponse(item.itemId(), item.productId(), item.cardId(),
				item.singleCard(), item.shopId(), item.shopName(), item.name(), item.cardNumber(),
				item.gameName(), item.seriesName(), item.type(), item.language(), item.rarity(),
				item.price(), item.sellerCount(), item.soldCount(), item.avgRating(), item.ratingCount(),
				item.favoriteCount(), imageUrl);
	}

	@Override
	@Transactional(readOnly = true)
	public PublicProductSearchOptionsResponse getFilterOptions(Long gameId) {
		Pageable optionsPage = PageRequest.of(0, FILTER_OPTIONS_PAGE_SIZE, Sort.by("name").ascending());
		List<GameResponse> games = gameCatalogService.search(null, true, optionsPage).content();
		List<SeriesResponse> series = seriesCatalogService.search(null, gameId, true, optionsPage).content();
		return new PublicProductSearchOptionsResponse(games, series);
	}
}
