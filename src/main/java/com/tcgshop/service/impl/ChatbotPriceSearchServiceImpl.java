package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ChatbotPriceOfferResponse;
import com.tcgshop.mapper.ChatbotPriceSearchMapper;
import com.tcgshop.repository.ChatbotPriceCandidateProjection;
import com.tcgshop.repository.ChatbotPriceSearchRepository;
import com.tcgshop.service.ChatbotPriceSearchService;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("permitAll()")
@RequiredArgsConstructor
public class ChatbotPriceSearchServiceImpl implements ChatbotPriceSearchService {
	private static final int MAX_CANDIDATES = 100;
	private final ChatbotPriceSearchRepository repository;
	private final ChatbotPriceSearchMapper mapper;
	private final PromotionService promotionService;

	@Override
	@Transactional(readOnly = true)
	public List<ChatbotPriceOfferResponse> search(BigDecimal minPrice, BigDecimal maxPrice,
			BigDecimal targetPrice, int limit) {
		if (minPrice == null || maxPrice == null || targetPrice == null || minPrice.signum() < 0
				|| maxPrice.compareTo(minPrice) < 0 || limit < 1) {
			return List.of();
		}
		List<ChatbotPriceCandidateProjection> candidates = repository.findPublicOffersInPriceRange(
				minPrice, maxPrice, targetPrice, PageRequest.of(0, MAX_CANDIDATES));
		if (candidates.isEmpty()) return List.of();

		Set<Long> productIds = new LinkedHashSet<>();
		candidates.forEach(candidate -> productIds.add(candidate.getProductId()));
		Map<Long, BigDecimal> effectivePrices = promotionService.getEffectivePrices(productIds);
		return candidates.stream()
				.map(candidate -> mapper.toResponse(candidate,
						effectivePrices.getOrDefault(candidate.getProductId(), candidate.getPrice())))
				.sorted(Comparator.comparing((ChatbotPriceOfferResponse offer) ->
						 offer.price().subtract(targetPrice).abs()).thenComparing(ChatbotPriceOfferResponse::productId))
				.limit(limit)
				.toList();
	}
}
