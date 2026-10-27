package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PlatformPromotionRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.dto.response.PromotionTargetOptionResponse;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PlatformMarketingMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PlatformPromotionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PlatformPromotionServiceImpl implements PlatformPromotionService {

	private final PromotionRepository promotionRepository;
	private final GameRepository gameRepository;
	private final SeriesRepository seriesRepository;
	private final ProductRepository productRepository;
	private final PlatformMarketingMapper marketingMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PlatformPromotionResponse> search(
			PromotionType type, ActiveStatus status, Pageable pageable) {
		ensureManagerOrAdmin();
		return PageResponse.from(
				promotionRepository.searchPlatformPromotions(type, status, pageable),
				this::toListResponse);
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
		if (promotionId == null && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Chỉ Admin được tạo mới chương trình toàn sàn");
		}
		validateRequest(request);

		Promotion promotion = promotionId == null ? new Promotion() : findPlatformPromotion(promotionId);
		Game game = null;
		Series series = null;
		HashSet<Product> products = new HashSet<>();

		if (request.getType() == PromotionType.PRODUCT_DISCOUNT) {
			if (request.getGameId() != null) {
				game = gameRepository.findById(request.getGameId())
						.filter(Game::isActive)
						.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy game đang hoạt động"));
			} else if (request.getSeriesId() != null) {
				series = seriesRepository.findById(request.getSeriesId())
						.filter(selectedSeries -> selectedSeries.isActive() && selectedSeries.getGame().isActive())
						.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy series đang hoạt động"));
			} else {
				List<Long> productIds = request.getProductIds().stream().distinct().toList();
				products.addAll(productRepository.findAvailableForPromotionOptions(ProductStatus.ACTIVE).stream()
						.filter(product -> productIds.contains(product.getId()))
						.toList());
				if (products.size() != productIds.size()) {
					throw new BadRequestException("Một hoặc nhiều sản phẩm không còn khả dụng");
				}
			}
		}

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
		return seriesRepository.findActivePromotionOptions().stream()
				.map(series -> new PromotionTargetOptionResponse(
						series.getId(), series.getName(), series.getGame().getName()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionTargetOptionResponse> getProductOptions() {
		ensureManagerOrAdmin();
		return productRepository.findAvailableForPromotionOptions(ProductStatus.ACTIVE).stream()
				.map(product -> new PromotionTargetOptionResponse(
						product.getId(), product.getName(),
						product.getShop().getName() + " · " + product.getPrice() + " ₫"))
				.toList();
	}

	private void validateRequest(PlatformPromotionRequest request) {
		if (request == null || request.getType() == null
				|| request.getTitle() == null || request.getTitle().isBlank()
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
			int targets = (request.getGameId() != null ? 1 : 0)
					+ (request.getSeriesId() != null ? 1 : 0)
					+ (request.getProductIds() != null && !request.getProductIds().isEmpty() ? 1 : 0);
			if (targets != 1) {
				throw new BadRequestException("Khuyến mãi sản phẩm phải chọn đúng một phạm vi: game, series hoặc sản phẩm");
			}
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
			if (request.getGameId() != null || request.getSeriesId() != null
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
		return promotion.getProducts().stream()
				.map(Product::getName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.reduce((first, second) -> first + ", " + second)
				.orElse("Chưa chọn sản phẩm");
	}

	private Promotion findPlatformPromotion(Long promotionId) {
		return promotionRepository.findPlatformPromotionById(promotionId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi toàn sàn"));
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền xem khuyến mãi toàn sàn");
		}
	}

}
