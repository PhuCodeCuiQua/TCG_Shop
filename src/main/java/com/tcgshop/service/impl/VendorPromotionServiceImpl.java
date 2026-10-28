package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorPromotionRequest;
import com.tcgshop.dto.response.VendorPromotionProductOptionResponse;
import com.tcgshop.dto.response.VendorPromotionResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorPromotionMapper;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorPromotionProductRepository;
import com.tcgshop.repository.VendorPromotionRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.VendorPromotionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorPromotionServiceImpl implements VendorPromotionService {
	private static final BigDecimal MIN_DISCOUNT = new BigDecimal("1.00");
	private static final BigDecimal MAX_DISCOUNT = new BigDecimal("90.00");

	private final VendorPromotionRepository promotionRepository;
	private final VendorPromotionProductRepository productRepository;
	private final ShopRepository shopRepository;
	private final VendorPromotionMapper mapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Page<VendorPromotionResponse> getCurrentShopPromotions(Pageable pageable) {
		Shop shop = getCurrentShop();
		return promotionRepository.findAllByShopId(shop.getId(), pageable)
				.map(this::toListResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public VendorPromotionResponse getCurrentShopPromotion(Long promotionId) {
		return toResponse(findOwnedPromotion(promotionId));
	}

	@Override
	@Transactional
	public VendorPromotionResponse saveCurrentShopPromotion(Long promotionId, VendorPromotionRequest request) {
		validateRequest(request);
		Shop shop = getCurrentShop();
		Promotion promotion = promotionId == null ? new Promotion() : findOwnedPromotion(promotionId);
		List<Long> productIds = request.getProductIds().stream().distinct().toList();
		List<Product> products = productRepository.findOwnedProductsByIds(shop.getId(), productIds);
		if (products.size() != productIds.size()) {
			throw new BadRequestException("Có sản phẩm không thuộc chi nhánh hoặc không còn khả dụng");
		}
		validateNoOverlap(shop.getId(), promotionId, request.getStartDate(), request.getEndDate(), productIds);

		promotion.setShop(shop);
		promotion.setTitle(request.getTitle().trim());
		promotion.setType(PromotionType.PRODUCT_DISCOUNT);
		promotion.setDiscountPercent(request.getDiscountPercent());
		promotion.setDiscountAmount(null);
		promotion.setMinOrderValue(null);
		promotion.setGame(null);
		promotion.setSeries(null);
		promotion.setProducts(new HashSet<>(products));
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		if (promotion.getStatus() == null) promotion.setStatus(ActiveStatus.ACTIVE);
		return toResponse(promotionRepository.save(promotion));
	}

	@Override
	@Transactional
	public VendorPromotionResponse toggleCurrentShopPromotion(Long promotionId) {
		Promotion promotion = findOwnedPromotion(promotionId);
		ActiveStatus nextStatus = promotion.getStatus() == ActiveStatus.ACTIVE
				? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE;
		if (nextStatus == ActiveStatus.ACTIVE) {
			validateNoOverlap(promotion.getShop().getId(), promotion.getId(), promotion.getStartDate(),
					promotion.getEndDate(), promotion.getProducts().stream().map(Product::getId).toList());
		}
		promotion.setStatus(nextStatus);
		return toResponse(promotion);
	}

	@Override
	@Transactional
	public VendorPromotionResponse deactivateCurrentShopPromotion(Long promotionId) {
		Promotion promotion = findOwnedPromotion(promotionId);
		promotion.setStatus(ActiveStatus.INACTIVE);
		return toResponse(promotion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VendorPromotionProductOptionResponse> getCurrentShopProductOptions() {
		Shop shop = getCurrentShop();
		return productRepository.findPromotionOptionsByShopId(shop.getId()).stream()
				.map(mapper::toProductOption)
				.toList();
	}

	private void validateRequest(VendorPromotionRequest request) {
		if (request == null || request.getTitle() == null || request.getTitle().isBlank()
				|| request.getDiscountPercent() == null || request.getStartDate() == null
				|| request.getEndDate() == null || request.getProductIds() == null
				|| request.getProductIds().isEmpty()) {
			throw new BadRequestException("Vui lòng nhập đầy đủ thông tin chương trình khuyến mãi");
		}
		if (request.getDiscountPercent().compareTo(MIN_DISCOUNT) < 0
				|| request.getDiscountPercent().compareTo(MAX_DISCOUNT) > 0) {
			throw new BadRequestException("Phần trăm giảm phải từ 1% đến 90%");
		}
		if (!request.getEndDate().isAfter(request.getStartDate())) {
			throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
		}
	}

	private void validateNoOverlap(Long shopId, Long excludedPromotionId, LocalDateTime startDate,
			LocalDateTime endDate, List<Long> productIds) {
		if (!productIds.isEmpty() && promotionRepository.countOverlappingActivePromotions(
				shopId, PromotionType.PRODUCT_DISCOUNT, ActiveStatus.ACTIVE, excludedPromotionId,
				startDate, endDate, productIds) > 0) {
			throw new BadRequestException("Một sản phẩm không thể thuộc hai chương trình khuyến mãi có thời gian trùng nhau");
		}
	}

	private Promotion findOwnedPromotion(Long promotionId) {
		Shop currentShop = getCurrentShop();
		Promotion promotion = promotionRepository.findByIdWithShopOwnerAndProducts(promotionId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình khuyến mãi"));
		if (promotion.getShop() == null || promotion.getShop().getOwner() == null
				|| !currentUser.getId().equals(promotion.getShop().getOwner().getId())
				|| !currentShop.getId().equals(promotion.getShop().getId())) {
			throw new ForbiddenException("Bạn không có quyền quản lý khuyến mãi của chi nhánh khác");
		}
		return promotion;
	}

	private Shop getCurrentShop() {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ chủ chi nhánh mới được quản lý khuyến mãi");
		}
		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh của tài khoản hiện tại"));
		if (shop.getStatus() != ShopStatus.APPROVED) {
			throw new ForbiddenException("Chỉ chi nhánh đã được duyệt mới được quản lý khuyến mãi");
		}
		return shop;
	}

	private VendorPromotionResponse toResponse(Promotion promotion) {
		VendorPromotionResponse mapped = mapper.toResponse(promotion);
		return new VendorPromotionResponse(mapped.id(), mapped.title(), mapped.discountPercent(),
				mapped.startDate(), mapped.endDate(), mapped.status(), displayStatus(promotion), mapped.productIds());
	}

	private VendorPromotionResponse toListResponse(Promotion promotion) {
		VendorPromotionResponse mapped = mapper.toListResponse(promotion);
		return new VendorPromotionResponse(mapped.id(), mapped.title(), mapped.discountPercent(),
				mapped.startDate(), mapped.endDate(), mapped.status(), displayStatus(promotion), List.of());
	}

	private String displayStatus(Promotion promotion) {
		LocalDateTime now = LocalDateTime.now();
		if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(now)) return "Đã kết thúc";
		if (promotion.getStatus() == ActiveStatus.INACTIVE) return "Tạm dừng";
		if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) return "Sắp diễn ra";
		return "Đang chạy";
	}
}
