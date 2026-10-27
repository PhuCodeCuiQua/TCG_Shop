package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ProductDetailResponse;
import com.tcgshop.dto.response.ProductDetailResponse.OfferItem;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.ProductDetailRepository;
import com.tcgshop.service.ProductDetailService;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductDetailServiceImpl implements ProductDetailService {
	private final ProductDetailRepository repository;
	private final PromotionService promotionService;

	public ProductDetailServiceImpl(ProductDetailRepository repository, PromotionService promotionService) {
		this.repository = repository;
		this.promotionService = promotionService;
	}

	@Override
	public ProductDetailResponse getDetail(Long productId) {
		Product product = repository.findDetailById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
		if (!isVisible(product)) {
			throw new ResourceNotFoundException("Sản phẩm", productId);
		}

		Card card = product.getCard();
		List<Product> offerProducts = new ArrayList<>();
		if (card != null) {
			offerProducts.addAll(repository.findOffersByCardId(card.getId()));
		}
		// Sản phẩm đang xem (kể cả hết hàng) luôn có mặt trong danh sách chọn người bán.
		boolean present = offerProducts.stream().anyMatch(o -> o.getId().equals(product.getId()));
		if (!present) {
			offerProducts.add(product);
		}

		Set<Long> ids = new HashSet<>();
		offerProducts.forEach(o -> ids.add(o.getId()));
		Map<Long, BigDecimal> effective = promotionService.getEffectivePrices(ids);

		List<OfferItem> offers = offerProducts.stream().map(o -> toOffer(o, effective)).toList();
		BigDecimal lowest = offers.stream()
				.filter(o -> o.stock() > 0)
				.map(OfferItem::effectivePrice)
				.filter(Objects::nonNull)
				.min(BigDecimal::compareTo)
				.orElse(null);

		int stock = stockOf(product);
		Series series = card != null ? card.getSeries() : product.getSeries();
		return new ProductDetailResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				imageOf(product, card),
				card != null,
				card == null ? null : card.getId(),
				series == null ? null : series.getName(),
				series == null || series.getGame() == null ? null : series.getGame().getName(),
				card == null ? null : card.getCardNumber(),
				card == null || card.getRarity() == null ? null : card.getRarity().name(),
				product.getCardCondition() == null ? null : product.getCardCondition().name(),
				product.getLanguage() == null ? null : product.getLanguage().name(),
				product.getPrice(),
				effective.getOrDefault(product.getId(), product.getPrice()),
				stock,
				stock > 0 && product.getStatus() == ProductStatus.ACTIVE,
				product.getSoldCount() == null ? 0 : product.getSoldCount(),
				marketPriceOf(card),
				lowest,
				product.getShop().getId(),
				product.getShop().getName(),
				product.getShop().getLogoUrl(),
				offers);
	}

	/** ACTIVE và OUT_OF_STOCK vẫn xem được; INACTIVE, BANNED, đã xóa hoặc shop chưa APPROVED thì ẩn. */
	private boolean isVisible(Product product) {
		boolean statusOk = product.getStatus() == ProductStatus.ACTIVE
				|| product.getStatus() == ProductStatus.OUT_OF_STOCK;
		boolean notDeleted = product.getDeleted() == null || !product.getDeleted();
		boolean shopOk = product.getShop() != null && product.getShop().getStatus() == ShopStatus.APPROVED;
		return statusOk && notDeleted && shopOk;
	}

	private OfferItem toOffer(Product p, Map<Long, BigDecimal> effective) {
		return new OfferItem(
				p.getId(),
				p.getShop().getId(),
				p.getShop().getName(),
				p.getShop().getLogoUrl(),
				p.getCardCondition() == null ? null : p.getCardCondition().name(),
				p.getLanguage() == null ? null : p.getLanguage().name(),
				p.getPrice(),
				effective.getOrDefault(p.getId(), p.getPrice()),
				p.getStatus() == ProductStatus.ACTIVE ? stockOf(p) : 0);
	}

	private int stockOf(Product product) {
		return product.getStock() == null ? 0 : product.getStock();
	}

	private BigDecimal marketPriceOf(Card card) {
		if (card == null) {
			return null;
		}
		List<PriceHistory> latest = repository.findLatestPrices(card.getId(), PageRequest.of(0, 1));
		if (!latest.isEmpty() && latest.get(0).getMarketPrice() != null) {
			return latest.get(0).getMarketPrice();
		}
		return card.getMarketPrice();
	}

	private String imageOf(Product product, Card card) {
		return com.tcgshop.util.ProductImageUtil.thumbnail(product);
	}
}
