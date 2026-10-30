package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PlatformPromotionRequest;
import com.tcgshop.dto.request.PlatformPromotionScope;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.dto.response.PublicShopOptionResponse;
import com.tcgshop.dto.response.PromotionTargetOptionResponse;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PlatformMarketingMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PlatformPromotionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PlatformPromotionServiceImpl implements PlatformPromotionService {

	private static final int PRODUCT_OPTION_PAGE_SIZE = 100;

	private final PromotionRepository promotionRepository;
	private final GameRepository gameRepository;
	private final SeriesRepository seriesRepository;
	private final ProductRepository productRepository;
	private final PlatformMarketingMapper marketingMapper;
	private final CurrentUser currentUser;
	private final ShopRepository shopRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PlatformPromotionResponse> search(
			PromotionType type, ActiveStatus status, Pageable pageable) {
		return search(type, status, null, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PlatformPromotionResponse> search(
			PromotionType type, ActiveStatus status, Long shopId, Pageable pageable) {
		ensureManagerOrAdmin();
		var promotions = shopId == null
				? promotionRepository.searchPlatformPromotions(type, status, pageable)
				: promotionRepository.searchPlatformPromotionsForShop(type, status, shopId, pageable);
		return PageResponse.from(
				promotions,
				this::toListResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PublicShopOptionResponse> getApprovedShopOptions() {
		ensureManagerOrAdmin();
		return shopRepository.findPublicShopOptions();
	}

	@Override
	@Transactional(readOnly = true)
	public PlatformPromotionResponse getById(Long promotionId) {
		ensureManagerOrAdmin();
		return toResponse(findPlatformPromotion(promotionId));
	}

	@Override
	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public PlatformPromotionResponse save(Long promotionId, PlatformPromotionRequest request) {
		ensureManagerOrAdmin();
		validateRequest(request);

		Promotion promotion = promotionId == null ? new Promotion() : findPlatformPromotion(promotionId);
		Game game = null;
		Series series = null;
		HashSet<Product> products = new HashSet<>();

		if (request.getType() == PromotionType.PRODUCT_DISCOUNT) {
			switch (request.getScope()) {
				case ALL -> {
				}
				case GAME_SERIES -> {
					if (request.getSeriesId() != null) {
						series = findActiveSeries(request.getSeriesId(), request.getGameId());
						game = series.getGame();
					} else if (request.getGameId() != null) {
						game = gameRepository.findById(request.getGameId())
								.filter(Game::isActive)
								.orElseThrow(() -> new ResourceNotFoundException(
										"Không tìm thấy game đang hoạt động"));
					} else {
						throw new BadRequestException("Vui lòng chọn Game hoặc Series");
					}
					products.addAll(productRepository.findAvailableForPromotionOptionsByScope(
							ProductStatus.ACTIVE, request.getGameId(), request.getSeriesId(), null));
				}
				case PRODUCTS -> {
					List<Long> productIds = distinctProductIds(request);
					if (productIds.isEmpty()) {
						throw new BadRequestException("Vui lòng chọn ít nhất một sản phẩm");
					}
					List<Product> availableProducts = productRepository.findAvailableForPromotionOptionsByScope(
							ProductStatus.ACTIVE, null, null, null);
					products.addAll(availableProducts.stream()
							.filter(product -> productIds.contains(product.getId()))
							.toList());
					if (products.size() != productIds.size()) {
						throw new BadRequestException(
								"Một hoặc nhiều sản phẩm không còn khả dụng trong phạm vi đã chọn");
					}
				}
				case SHOP -> {
					if (request.getShopId() == null) {
						throw new BadRequestException("Vui lòng chọn chi nhánh");
					}
					Shop selectedShop = shopRepository.findApprovedNotDeletedById(request.getShopId())
							.orElseThrow(() -> new ResourceNotFoundException(
									"Không tìm thấy chi nhánh đang hoạt động"));
					products.addAll(productRepository.findAvailableForPromotionOptionsByScope(
							ProductStatus.ACTIVE, null, null, selectedShop.getId()));
					if (products.isEmpty()) {
						throw new BadRequestException("Chi nhánh chưa có sản phẩm đang hoạt động để áp dụng");
					}
				}
			}
		}

		// Promotion platform vẫn giữ shop_id = null; phạm vi Shop được lưu bằng các sản phẩm mục tiêu.
		promotion.setShop(null);
		promotion.setTitle(request.getTitle().trim());
		promotion.setType(request.getType());
		promotion.setDiscountPercent(request.getDiscountPercent());
		promotion.setDiscountAmount(request.getDiscountAmount());
		promotion.setMinOrderValue(request.getMinOrderValue());
		promotion.setGame(game);
		promotion.setSeries(series);
		promotion.setProducts(products);
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		promotion.setStatus(request.getStatus());
		return toResponse(promotionRepository.save(promotion));
	}

	private Series findActiveSeries(Long seriesId, Long gameId) {
		Series selectedSeries = seriesRepository.findById(seriesId)
				.filter(candidate -> candidate.isActive() && candidate.getGame().isActive())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy series đang hoạt động"));
		if (gameId != null && !gameId.equals(selectedSeries.getGame().getId())) {
			throw new BadRequestException("Series không thuộc game đã chọn");
		}
		return selectedSeries;
	}

	private List<Long> distinctProductIds(PlatformPromotionRequest request) {
		return request.getProductIds() == null
				? List.of()
				: request.getProductIds().stream().distinct().toList();
	}

	@Override
	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public PlatformPromotionResponse setStatus(Long promotionId, ActiveStatus status) {
		ensureManagerOrAdmin();
		if (status == null) {
			throw new BadRequestException("Vui lòng chọn trạng thái");
		}
		Promotion promotion = findPlatformPromotion(promotionId);
		promotion.setStatus(status);
		return toResponse(promotion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionTargetOptionResponse> getGameOptions() {
		ensureManagerOrAdmin();
		return gameRepository.findByActiveTrueOrderByNameAsc().stream()
				.map(game -> new PromotionTargetOptionResponse(game.getId(), game.getName(), game.getCode()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionTargetOptionResponse> getSeriesOptions() {
		ensureManagerOrAdmin();
		return mapSeriesOptions(seriesRepository.findActivePromotionOptions());
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("permitAll()")
	public List<PromotionTargetOptionResponse> getSeriesOptions(Long gameId) {
		return mapSeriesOptions(seriesRepository.findActivePromotionOptionsByGameId(gameId));
	}

	private List<PromotionTargetOptionResponse> mapSeriesOptions(List<Series> seriesOptions) {
		return seriesOptions.stream()
				.map(series -> new PromotionTargetOptionResponse(
						series.getId(), series.getName(), series.getGame().getName()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionTargetOptionResponse> getProductOptions() {
		ensureManagerOrAdmin();
		return mapProductOptions(productRepository.findAvailableForPromotionOptions(ProductStatus.ACTIVE));
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("permitAll()")
	public List<PromotionTargetOptionResponse> getProductOptions(Long seriesId) {
		return getProductOptions(null, seriesId);
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("permitAll()")
	public List<PromotionTargetOptionResponse> getProductOptions(Long gameId, Long seriesId) {
		return getProductOptions(gameId, seriesId, null);
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("permitAll()")
	public List<PromotionTargetOptionResponse> getProductOptions(Long gameId, Long seriesId, Long shopId) {
		return mapProductOptions(productRepository.findAvailableForPromotionOptionsByScope(
				ProductStatus.ACTIVE, gameId, seriesId, shopId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionTargetOptionResponse> getSelectedProductOptions(Collection<Long> productIds) {
		ensureManagerOrAdmin();
		if (productIds == null || productIds.isEmpty()) {
			return List.of();
		}
		return mapProductOptions(productRepository.findAvailableForPromotionOptionsByIdIn(
				ProductStatus.ACTIVE, productIds));
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("permitAll()")
	public List<PromotionTargetOptionResponse> searchProductOptions(
			Long gameId, Long seriesId, Long shopId, String keyword) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
		return mapProductOptions(productRepository.searchAvailablePromotionOptions(
				ProductStatus.ACTIVE, gameId, seriesId, shopId, normalizedKeyword,
				PageRequest.of(0, PRODUCT_OPTION_PAGE_SIZE)));
	}

	private List<PromotionTargetOptionResponse> mapProductOptions(List<Product> productOptions) {
		return productOptions.stream()
				.map(product -> new PromotionTargetOptionResponse(
						product.getId(), product.getName(),
						product.getShop().getName() + " · " + product.getPrice() + " ₫"))
				.toList();
	}

	private void validateRequest(PlatformPromotionRequest request) {
		if (request == null || request.getType() == null
				|| request.getTitle() == null || request.getTitle().isBlank()
				|| request.getScope() == null
				|| request.getStartDate() == null || request.getEndDate() == null
				|| request.getStatus() == null) {
			throw new BadRequestException("Vui lòng nhập đầy đủ thông tin khuyến mãi");
		}
		if (!request.getStartDate().isBefore(request.getEndDate())) {
			throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
		}
		if (request.getMinOrderValue() != null && request.getMinOrderValue().signum() < 0) {
			throw new BadRequestException("Đơn tối thiểu không được âm");
		}
		if (request.getType() == PromotionType.PRODUCT_DISCOUNT) {
			validatePercent(request.getDiscountPercent());
			if (request.getDiscountAmount() != null) {
				throw new BadRequestException("Khuyến mãi sản phẩm chỉ hỗ trợ giảm theo phần trăm");
			}
			if (request.getMinOrderValue() != null) {
				throw new BadRequestException("Điều kiện đơn tối thiểu chỉ áp dụng cho khuyến mãi phí vận chuyển");
			}
		} else {
			boolean percent = request.getDiscountPercent() != null;
			boolean fixedAmount = request.getDiscountAmount() != null;
			if (percent == fixedAmount) {
				throw new BadRequestException("Khuyến mãi vận chuyển cần chọn giảm theo phần trăm hoặc số tiền");
			}
			if (percent) {
				validatePercent(request.getDiscountPercent());
			} else if (request.getDiscountAmount().signum() <= 0) {
				throw new BadRequestException("Số tiền giảm phí vận chuyển phải lớn hơn 0");
			}
			if (request.getMinOrderValue() == null || request.getMinOrderValue().signum() <= 0) {
				throw new BadRequestException("Vui lòng nhập giá trị đơn hàng tối thiểu cho ưu đãi vận chuyển");
			}
			if (request.getScope() != PlatformPromotionScope.ALL
					|| request.getGameId() != null || request.getSeriesId() != null || request.getShopId() != null
					|| (request.getProductIds() != null && !request.getProductIds().isEmpty())) {
				throw new BadRequestException("Khuyến mãi vận chuyển áp dụng theo điều kiện đơn hàng, không chọn sản phẩm");
			}
		}
	}

	private void validatePercent(BigDecimal percent) {
		if (percent == null || percent.signum() <= 0 || percent.compareTo(new BigDecimal("100.00")) > 0) {
			throw new BadRequestException("Phần trăm giảm phải lớn hơn 0% và không vượt quá 100%");
		}
	}

	private PlatformPromotionResponse toResponse(Promotion promotion) {
		PlatformPromotionResponse response = marketingMapper.toPromotionResponse(promotion);
		return new PlatformPromotionResponse(
				response.id(), response.title(), response.type(), response.discountPercent(),
				response.discountAmount(), response.minOrderValue(), response.gameId(), response.gameName(),
				response.seriesId(), response.seriesName(), response.productIds(),
				targetSummary(promotion), response.startDate(), response.endDate(), response.status());
	}

	private PlatformPromotionResponse toListResponse(Promotion promotion) {
		PlatformPromotionResponse response = marketingMapper.toPromotionListResponse(promotion);
		String target = "Sản phẩm đã chọn";
		if (promotion.getType() == PromotionType.SHIPPING_DISCOUNT) {
			target = "Đơn từ " + promotion.getMinOrderValue() + " ₫";
		} else if (promotion.getGame() != null) {
			target = "Game: " + promotion.getGame().getName();
		} else if (promotion.getSeries() != null) {
			target = "Series: " + promotion.getSeries().getName();
		} else if (promotion.getGame() == null && promotion.getProducts().isEmpty()) {
			target = "Toàn bộ sản phẩm đang hoạt động";
		} else if (!promotion.getProducts().isEmpty()) {
			target = promotion.getProducts().size() + " sản phẩm cụ thể";
		}
		return new PlatformPromotionResponse(
				response.id(), response.title(), response.type(), response.discountPercent(),
				response.discountAmount(), response.minOrderValue(), response.gameId(), response.gameName(),
				response.seriesId(), response.seriesName(), List.of(), target,
				response.startDate(), response.endDate(), response.status());
	}

	private String targetSummary(Promotion promotion) {
		if (promotion.getType() == PromotionType.SHIPPING_DISCOUNT) {
			return "Đơn hàng từ " + promotion.getMinOrderValue() + " ₫";
		}
		if (promotion.getGame() != null) {
			return "Game: " + promotion.getGame().getName();
		}
		if (promotion.getSeries() != null) {
			return "Series: " + promotion.getSeries().getName();
		}
		if (promotion.getProducts().isEmpty()) {
			return "Toàn bộ sản phẩm đang hoạt động";
		}
		return promotion.getProducts().size() + " sản phẩm cụ thể";
	}

	private Promotion findPlatformPromotion(Long promotionId) {
		return promotionRepository.findPlatformPromotionById(promotionId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi hệ thống"));
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền xem khuyến mãi hệ thống");
		}
	}

}
