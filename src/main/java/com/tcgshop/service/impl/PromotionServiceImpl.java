package com.tcgshop.service.impl;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.PromotionPricingProductRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.repository.VendorPromotionRepository;
import com.tcgshop.service.PromotionService;
import com.tcgshop.util.MoneyUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionServiceImpl implements PromotionService {
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private final PromotionPricingProductRepository productRepository;
	private final VendorPromotionRepository vendorPromotionRepository;
	private final PromotionRepository platformPromotionRepository;

	public PromotionServiceImpl(PromotionPricingProductRepository productRepository,
			VendorPromotionRepository vendorPromotionRepository,
			PromotionRepository platformPromotionRepository) {
		this.productRepository = productRepository;
		this.vendorPromotionRepository = vendorPromotionRepository;
		this.platformPromotionRepository = platformPromotionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getEffectivePrice(Long productId) {
		return getEffectivePrices(List.of(productId)).get(productId);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, BigDecimal> getEffectivePrices(Collection<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) return Map.of();
		List<Long> distinctIds = productIds.stream().distinct().toList();
		List<Product> products = productRepository.findAllForPromotionPricing(distinctIds);
		Map<Long, Product> productsById = products.stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));
		for (Long productId : distinctIds) {
			if (!productsById.containsKey(productId)) {
				throw new ResourceNotFoundException("Sản phẩm", productId);
			}
		}

		LocalDateTime now = LocalDateTime.now();
		List<Promotion> shopPromotions = vendorPromotionRepository.findActiveShopPromotionsForProducts(
				PromotionType.PRODUCT_DISCOUNT, ActiveStatus.ACTIVE, now, distinctIds);
		List<Promotion> platformPromotions = platformPromotionRepository.findActivePlatformPromotions(
				ActiveStatus.ACTIVE, PromotionType.PRODUCT_DISCOUNT, now);

		Map<Long, BigDecimal> effectivePrices = new HashMap<>();
		for (Product product : products) {
			BigDecimal shopDiscount = maxDiscountForProduct(shopPromotions,
					promotion -> promotion.getShop() != null && product.getShop() != null
							&& promotion.getShop().getId().equals(product.getShop().getId())
							&& containsProduct(promotion, product.getId()));
			BigDecimal platformDiscount = maxDiscountForProduct(platformPromotions,
					promotion -> appliesToPlatformTarget(promotion, product));
			BigDecimal shopPrice = applyDiscount(product.getPrice(), shopDiscount);
			BigDecimal platformPrice = applyDiscount(product.getPrice(), platformDiscount);
			BigDecimal effectivePrice = shopDiscount.signum() == 0 ? platformPrice
					: platformDiscount.signum() == 0 ? shopPrice
							: shopPrice.min(platformPrice);
			effectivePrices.put(product.getId(), effectivePrice);
		}
		return Map.copyOf(effectivePrices);
	}

	private BigDecimal maxDiscountForProduct(List<Promotion> promotions,
			java.util.function.Predicate<Promotion> applies) {
		return promotions.stream()
				.filter(applies)
				.map(Promotion::getDiscountPercent)
				.filter(discount -> discount != null && discount.signum() > 0)
				.max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	private boolean appliesToPlatformTarget(Promotion promotion, Product product) {
		if (promotion.getProducts() != null && !promotion.getProducts().isEmpty()
				&& containsProduct(promotion, product.getId())) return true;
		Series productSeries = product.getSeries();
		if (productSeries == null && product.getCard() != null) productSeries = product.getCard().getSeries();
		if (promotion.getSeries() != null && productSeries != null
				&& promotion.getSeries().getId().equals(productSeries.getId())) return true;
		if (promotion.getGame() != null) {
			return productSeries != null && productSeries.getGame() != null
					&& promotion.getGame().getId().equals(productSeries.getGame().getId());
		}
		return promotion.getSeries() == null
				&& (promotion.getProducts() == null || promotion.getProducts().isEmpty())
				&& product.getStatus() == ProductStatus.ACTIVE;
	}

	private boolean containsProduct(Promotion promotion, Long productId) {
		return promotion.getProducts().stream().anyMatch(target -> productId.equals(target.getId()));
	}

	private BigDecimal applyDiscount(BigDecimal originalPrice, BigDecimal discount) {
		if (originalPrice == null) return BigDecimal.ZERO;
		if (discount.signum() == 0) return originalPrice;
		BigDecimal effective = originalPrice.multiply(HUNDRED.subtract(discount))
				.divide(HUNDRED, 8, RoundingMode.HALF_UP);
		return MoneyUtils.round(effective);
	}
}
