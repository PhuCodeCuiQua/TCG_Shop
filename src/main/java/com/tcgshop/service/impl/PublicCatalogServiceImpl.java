package com.tcgshop.service.impl;

import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.GameSeriesResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicCatalogProductResponse;
import com.tcgshop.dto.response.PublicSeriesPageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PublicCatalogProductMapper;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.repository.ProductThumbnailProjection;
import com.tcgshop.repository.SeriesCoverProjection;
import com.tcgshop.repository.PublicCatalogProductRepository;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PublicCatalogService;
import com.tcgshop.service.SeriesCatalogService;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicCatalogServiceImpl implements PublicCatalogService {
	private static final String GAME_NOT_FOUND_MESSAGE = "Không tìm thấy game";
	private static final String SERIES_NOT_FOUND_MESSAGE = "Không tìm thấy series";

	private final GameCatalogService gameCatalogService;
	private final SeriesCatalogService seriesCatalogService;
	private final PublicCatalogProductRepository publicCatalogProductRepository;
	private final ProductImageRepository productImageRepository;
	private final PublicCatalogProductMapper publicCatalogProductMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<GameResponse> getActiveGames(Pageable pageable) {
		return gameCatalogService.search(null, true, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public GameResponse getActiveGame(Long gameId) {
		GameResponse game = gameCatalogService.getById(gameId);
		if (!game.active()) {
			throw new ResourceNotFoundException(GAME_NOT_FOUND_MESSAGE);
		}
		return game;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<GameSeriesResponse> getActiveSeries(Long gameId, Pageable pageable) {
		getActiveGame(gameId);
		PageResponse<SeriesResponse> seriesPage = seriesCatalogService.search(null, gameId, true, pageable);
		if (seriesPage.content().isEmpty()) {
			return new PageResponse<>(java.util.List.of(), seriesPage.page(), seriesPage.size(),
					seriesPage.totalElements(), seriesPage.totalPages(), seriesPage.first(), seriesPage.last());
		}
		Map<Long, String> covers = productImageRepository.findSeriesCovers(
				seriesPage.content().stream().map(SeriesResponse::id).toList()).stream()
				.filter(cover -> cover.getImageUrl() != null && !cover.getImageUrl().isBlank())
				.collect(Collectors.toMap(SeriesCoverProjection::getSeriesId,
						SeriesCoverProjection::getImageUrl, (first, ignored) -> first));
		return new PageResponse<>(seriesPage.content().stream()
				.map(series -> new GameSeriesResponse(series.id(), series.gameId(), series.gameName(),
						series.name(), series.code(), series.releaseDate(), series.active(), covers.get(series.id())))
				.toList(), seriesPage.page(), seriesPage.size(), seriesPage.totalElements(),
				seriesPage.totalPages(), seriesPage.first(), seriesPage.last());
	}

	@Override
	@Transactional(readOnly = true)
	public PublicSeriesPageResponse getActiveSeriesProducts(Long seriesId, Pageable pageable) {
		SeriesResponse series = seriesCatalogService.getById(seriesId);
		if (!series.active()) {
			throw new ResourceNotFoundException(SERIES_NOT_FOUND_MESSAGE);
		}
		GameResponse game = getActiveGame(series.gameId());
		var productPage = publicCatalogProductRepository.findPublicProductsBySeriesId(seriesId, pageable);
		var mappedProducts = productPage.getContent().stream()
				.map(publicCatalogProductMapper::toResponse)
				.toList();
		Map<Long, String> productImages;
		Map<Long, String> seriesFallbacks;
		if (mappedProducts.isEmpty()) {
			productImages = Map.of();
			seriesFallbacks = Map.of();
		} else {
			var productIds = mappedProducts.stream().map(PublicCatalogProductResponse::id).toList();
			productImages = thumbnailsByProductId(productImageRepository.findThumbnails(productIds));
			seriesFallbacks = thumbnailsByProductId(productImageRepository.findSeriesFallbacks(productIds));
		}
		var productsWithImages = mappedProducts.stream()
				.map(product -> withResolvedImage(product, productImages.get(product.id()), seriesFallbacks.get(product.id())))
				.toList();
		PageResponse<PublicCatalogProductResponse> products = new PageResponse<>(productsWithImages,
				productPage.getNumber(), productPage.getSize(), productPage.getTotalElements(),
				productPage.getTotalPages(), productPage.isFirst(), productPage.isLast());
		return new PublicSeriesPageResponse(game, series, products);
	}

	private Map<Long, String> thumbnailsByProductId(Collection<? extends ProductThumbnailProjection> thumbnails) {
		return thumbnails.stream()
				.filter(thumbnail -> thumbnail.getImageUrl() != null && !thumbnail.getImageUrl().isBlank())
				.collect(Collectors.toMap(ProductThumbnailProjection::getProductId,
						ProductThumbnailProjection::getImageUrl, (first, ignored) -> first));
	}

	private PublicCatalogProductResponse withResolvedImage(
			PublicCatalogProductResponse product, String productImage, String seriesFallback) {
		String imageUrl = productImage == null || productImage.isBlank() ? seriesFallback : productImage;
		return new PublicCatalogProductResponse(product.id(), product.shopId(), product.shopName(), product.name(),
				product.cardName(), product.cardNumber(), product.type(), product.cardCondition(), product.price(),
				product.stock(), product.soldCount(), imageUrl);
	}
}
