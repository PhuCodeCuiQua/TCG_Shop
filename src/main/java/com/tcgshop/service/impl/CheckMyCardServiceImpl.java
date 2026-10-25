package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CardOfferResponse;
import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.dto.response.CheckMyCardResponse;
import com.tcgshop.dto.response.PriceChartResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.service.CardOfferService;
import com.tcgshop.service.CheckMyCardService;
import com.tcgshop.service.PriceChartService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CheckMyCardServiceImpl implements CheckMyCardService {
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final int CHART_RANGE_DAYS = 30;

	private final CardRepository cardRepository;
	private final CardOfferService cardOfferService;
	private final PriceChartService priceChartService;

	public CheckMyCardServiceImpl(CardRepository cardRepository, CardOfferService cardOfferService,
			PriceChartService priceChartService) {
		this.cardRepository = cardRepository;
		this.cardOfferService = cardOfferService;
		this.priceChartService = priceChartService;
	}

	@Override
	public CheckMyCardResponse check(Long cardId, CardCondition condition, CardLanguage language) {
		Card card = cardRepository.findByIdAndActiveTrue(cardId)
				.filter(found -> found.getSeries() != null
						&& found.getSeries().isActive()
						&& found.getSeries().getGame() != null
						&& found.getSeries().getGame().isActive())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ đang được hiển thị"));

		CardOffersResponse allOffers = cardOfferService.getOffers(cardId, condition, "price", "asc");
		List<CardOfferResponse> offers = allOffers.offers().stream()
				.filter(offer -> language == null || language.name().equals(offer.language()))
				.toList();
		CardOfferResponse cheapest = offers.stream()
				.filter(offer -> offer.effectivePrice() != null)
				.min((left, right) -> left.effectivePrice().compareTo(right.effectivePrice()))
				.orElse(null);
		Set<Long> sellerIds = offers.stream()
				.map(CardOfferResponse::shopId)
				.collect(Collectors.toSet());
		PriceChartResponse chart = priceChartService.getChart(cardId, CHART_RANGE_DAYS);
		BigDecimal lowestSellingPrice = cheapest == null ? null : cheapest.effectivePrice();

		return new CheckMyCardResponse(
				card.getId(),
				card.getName(),
				card.getCardNumber(),
				card.getImageUrl(),
				card.getMarketPrice(),
				lowestSellingPrice,
				sellerIds.size(),
				percentDifference(lowestSellingPrice, card.getMarketPrice()),
				cheapest == null ? null : cheapest.productId(),
				chart.points());
	}

	private BigDecimal percentDifference(BigDecimal sellingPrice, BigDecimal marketPrice) {
		if (sellingPrice == null || marketPrice == null || marketPrice.signum() <= 0) {
			return null;
		}
		return sellingPrice.subtract(marketPrice)
				.multiply(ONE_HUNDRED)
				.divide(marketPrice, 2, RoundingMode.HALF_UP);
	}
}
