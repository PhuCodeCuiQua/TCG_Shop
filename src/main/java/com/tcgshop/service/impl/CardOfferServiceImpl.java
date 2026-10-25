package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CardOfferResponse;
import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.repository.CardOfferRepository;
import com.tcgshop.service.CardOfferService;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CardOfferServiceImpl implements CardOfferService {
	private static final Set<String> SORTS = Set.of("price", "condition", "rating");
	private static final Set<String> DIRS = Set.of("asc", "desc");

	private final CardOfferRepository repository;
	private final PromotionService promotionService;

	public CardOfferServiceImpl(CardOfferRepository repository, PromotionService promotionService) {
		this.repository = repository;
		this.promotionService = promotionService;
	}

	@Override
	public CardOffersResponse getOffers(Long cardId, CardCondition condition, String sort, String dir) {
		String sortKey = sort == null ? "price" : sort.toLowerCase();
		String direction = dir == null ? "asc" : dir.toLowerCase();
		if (!SORTS.contains(sortKey)) {
			throw new BadRequestException("Kiểu sắp xếp không hợp lệ");
		}
		if (!DIRS.contains(direction)) {
			throw new BadRequestException("Chiều sắp xếp không hợp lệ");
		}

		List<Product> all = repository.findAvailableByCardId(cardId);
		if (all.isEmpty()) {
			return new CardOffersResponse(List.of(), List.of());
		}

		List<String> availableConditions = all.stream()
				.map(Product::getCardCondition)
				.filter(c -> c != null)
				.distinct()
				.sorted(Comparator.comparingInt(CardCondition::ordinal))
				.map(CardCondition::name)
				.toList();

		List<Product> filtered = condition == null ? all
				: all.stream().filter(p -> p.getCardCondition() == condition).toList();

		Set<Long> productIds = new HashSet<>();
		filtered.forEach(p -> productIds.add(p.getId()));
		Map<Long, BigDecimal> effective = productIds.isEmpty() ? Map.of()
				: promotionService.getEffectivePrices(productIds);

		List<CardOfferResponse> offers = filtered.stream()
				.map(p -> toResponse(p, effective))
				.sorted(comparator(sortKey, direction))
				.toList();
		return new CardOffersResponse(offers, availableConditions);
	}

	private CardOfferResponse toResponse(Product p, Map<Long, BigDecimal> effective) {
		Shop shop = p.getShop();
		long ratingCount = shop.getRatingCount() == null ? 0 : shop.getRatingCount();
		BigDecimal rating = ratingCount > 0 ? shop.getAvgRating() : null;
		return new CardOfferResponse(
				p.getId(),
				shop.getId(),
				shop.getName(),
				rating,
				ratingCount,
				p.getCardCondition() == null ? null : p.getCardCondition().name(),
				p.getLanguage() == null ? null : p.getLanguage().name(),
				p.getPrice(),
				effective.getOrDefault(p.getId(), p.getPrice()),
				p.getStock() == null ? 0 : p.getStock());
	}

	/** Khóa chính theo yêu cầu, khóa phụ luôn là giá tăng dần rồi id để kết quả ổn định. */
	private Comparator<CardOfferResponse> comparator(String sort, String dir) {
		boolean desc = "desc".equals(dir);
		Comparator<CardOfferResponse> primary = switch (sort) {
			case "condition" -> keyed(o -> o.condition() == null ? null : CardCondition.valueOf(o.condition()).ordinal(), desc);
			case "rating" -> keyed(CardOfferResponse::shopRating, desc);
			default -> keyed(CardOfferResponse::effectivePrice, desc);
		};
		return primary
				.thenComparing(CardOfferResponse::effectivePrice, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(CardOfferResponse::productId);
	}

	/** Giá trị null luôn xếp cuối, kể cả khi sắp xếp giảm dần. */
	private <T extends Comparable<? super T>> Comparator<CardOfferResponse> keyed(
			Function<CardOfferResponse, T> extractor, boolean desc) {
		Comparator<T> keyComparator = desc ? Comparator.<T>naturalOrder().reversed() : Comparator.<T>naturalOrder();
		return Comparator.comparing(extractor, Comparator.nullsLast(keyComparator));
	}
}