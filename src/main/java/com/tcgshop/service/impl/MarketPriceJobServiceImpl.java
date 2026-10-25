package com.tcgshop.service.impl;

import com.tcgshop.dto.response.MarketPriceJobSummary;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.event.PriceUpdatedEvent;
import com.tcgshop.repository.AdminMarketPrice;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.DeliveredCardSales;
import com.tcgshop.repository.MarketPriceJobRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import com.tcgshop.service.MarketPriceJobService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketPriceJobServiceImpl implements MarketPriceJobService {
	private static final BigDecimal SIGNIFICANT_CHANGE_PERCENT = new BigDecimal("5.00");
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final int PRICE_SCALE = 2;
	private static final String JOB_SOURCE = "JOB";
	private static final String ADMIN_SOURCE = "ADMIN";

	private final CardRepository cardRepository;
	private final MarketPriceJobRepository marketPriceJobRepository;
	private final PriceHistoryRepository priceHistoryRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Override
	@Transactional
	public MarketPriceJobSummary runNow() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate today = LocalDate.now(clock);
		LocalDateTime since = now.minusDays(30);
		List<Card> cards = cardRepository.findAll();
		if (cards.isEmpty()) {
			log.info("Market price job completed: processed 0 cards, updated 0, published 0 events");
			return new MarketPriceJobSummary(0, 0, 0);
		}

		Map<Long, Card> cardsById = cards.stream()
				.collect(Collectors.toMap(Card::getId, Function.identity()));
		Map<Long, DeliveredCardSales> salesByCard = marketPriceJobRepository
				.findDeliveredCardSalesBetween(since, now).stream()
				.collect(Collectors.toMap(DeliveredCardSales::getCardId, Function.identity()));
		Map<Long, BigDecimal> adminPrices = latestAdminPrices(cardsById.keySet());
		Map<Long, PriceHistory> historiesByCard = priceHistoryRepository
				.findForCardsByRecordedDate(today, cardsById.keySet()).stream()
				.collect(Collectors.toMap(history -> history.getCard().getId(), Function.identity()));

		int updatedCards = 0;
		int publishedEvents = 0;
		for (Card card : cards) {
			PriceHistory history = historiesByCard.get(card.getId());
			if (history != null && ADMIN_SOURCE.equals(history.getSource())) {
				continue;
			}

			BigDecimal oldPrice = card.getMarketPrice();
			DeliveredCardSales sales = salesByCard.get(card.getId());
			BigDecimal averageSoldPrice = averageSoldPrice(sales);
			BigDecimal latestAdminPrice = adminPrices.get(card.getId());
			BigDecimal newPrice = averageSoldPrice != null ? averageSoldPrice
					: latestAdminPrice != null ? latestAdminPrice : oldPrice;

			if (history == null) {
				history = new PriceHistory();
				history.setCard(card);
				history.setRecordedDate(today);
				historiesByCard.put(card.getId(), history);
			}
			history.setMarketPrice(newPrice);
			history.setAvgPrice(averageSoldPrice);
			history.setSource(JOB_SOURCE);
			priceHistoryRepository.save(history);

			if (!samePrice(oldPrice, newPrice)) {
				card.setMarketPrice(newPrice);
				updatedCards++;
				if (isSignificantChange(oldPrice, newPrice)) {
					eventPublisher.publishEvent(new PriceUpdatedEvent(
							card.getId(), card.getName(), oldPrice, newPrice, today, JOB_SOURCE));
					publishedEvents++;
				}
			}
		}

		MarketPriceJobSummary summary = new MarketPriceJobSummary(cards.size(), updatedCards, publishedEvents);
		log.info("Market price job completed: processed {} cards, updated {}, published {} events",
				summary.processedCards(), summary.updatedCards(), summary.publishedEvents());
		return summary;
	}

	private Map<Long, BigDecimal> latestAdminPrices(Set<Long> cardIds) {
		List<AdminMarketPrice> rows = priceHistoryRepository.findLatestAdminPrices(cardIds);
		Map<Long, BigDecimal> prices = new HashMap<>();
		for (AdminMarketPrice row : rows) {
			prices.put(row.getCardId(), row.getMarketPrice());
		}
		return prices;
	}

	private BigDecimal averageSoldPrice(DeliveredCardSales sales) {
		if (sales == null || sales.getQuantity() == null || sales.getQuantity() <= 0
				|| sales.getSalesAmount() == null) {
			return null;
		}
		return sales.getSalesAmount().divide(
				BigDecimal.valueOf(sales.getQuantity()), PRICE_SCALE, RoundingMode.HALF_UP);
	}

	private boolean isSignificantChange(BigDecimal oldPrice, BigDecimal newPrice) {
		if (newPrice == null) {
			return false;
		}
		if (oldPrice == null || oldPrice.signum() <= 0) {
			return newPrice.signum() > 0;
		}
		BigDecimal differencePercent = newPrice.subtract(oldPrice).abs()
				.multiply(ONE_HUNDRED);
		return differencePercent.compareTo(oldPrice.abs().multiply(SIGNIFICANT_CHANGE_PERCENT)) >= 0;
	}

	private boolean samePrice(BigDecimal left, BigDecimal right) {
		return left == null ? right == null : right != null && left.compareTo(right) == 0;
	}
}
