package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.MarketPriceJobSummary;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.event.PriceUpdatedEvent;
import com.tcgshop.repository.AdminMarketPrice;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.DeliveredCardSales;
import com.tcgshop.repository.MarketPriceJobRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MarketPriceJobServiceImplTest {
	private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final Instant NOW = Instant.parse("2026-10-25T03:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
	private static final Long CARD_ID = 17L;

	@Mock
	private CardRepository cardRepository;
	@Mock
	private MarketPriceJobRepository marketPriceJobRepository;
	@Mock
	private PriceHistoryRepository priceHistoryRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	private MarketPriceJobServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new MarketPriceJobServiceImpl(
				cardRepository,
				marketPriceJobRepository,
				priceHistoryRepository,
				eventPublisher,
				Clock.fixed(NOW, ZONE));
	}

	@Test
	void runNow_usesQuantityWeightedDeliveredSalesAndEmitsSignificantChangeOnlyOnce() {
		Card card = card("1000.00");
		when(cardRepository.findAll()).thenReturn(List.of(card));
		when(marketPriceJobRepository.findDeliveredCardSalesBetween(
				LocalDateTime.of(2026, 9, 25, 10, 0), LocalDateTime.of(2026, 10, 25, 10, 0)))
				.thenReturn(List.of(sales("3300.00", 3)));
		when(priceHistoryRepository.findLatestAdminPrices(Set.of(CARD_ID))).thenReturn(List.of());
		PriceHistory todaysRecord = new PriceHistory();
		todaysRecord.setId(99L);
		todaysRecord.setCard(card);
		todaysRecord.setRecordedDate(TODAY);
		todaysRecord.setSource("JOB");
		when(priceHistoryRepository.findForCardsByRecordedDate(TODAY, Set.of(CARD_ID)))
				.thenReturn(List.of(), List.of(todaysRecord));

		MarketPriceJobSummary firstRun = service.runNow();
		MarketPriceJobSummary secondRun = service.runNow();

		assertThat(firstRun).isEqualTo(new MarketPriceJobSummary(1, 1, 1));
		assertThat(secondRun).isEqualTo(new MarketPriceJobSummary(1, 0, 0));
		assertThat(card.getMarketPrice()).isEqualByComparingTo("1100.00");
		assertThat(todaysRecord.getMarketPrice()).isEqualByComparingTo("1100.00");
		assertThat(todaysRecord.getAvgPrice()).isEqualByComparingTo("1100.00");
		assertThat(todaysRecord.getSource()).isEqualTo("JOB");
		ArgumentCaptor<PriceUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PriceUpdatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().oldPrice()).isEqualByComparingTo("1000.00");
		assertThat(eventCaptor.getValue().newPrice()).isEqualByComparingTo("1100.00");
		verify(priceHistoryRepository, org.mockito.Mockito.times(2))
				.findForCardsByRecordedDate(TODAY, Set.of(CARD_ID));
	}

	@Test
	void runNow_withoutSalesFallsBackToLatestAdminPriceAndDoesNotEmitBelowThresholdChange() {
		Card card = card("1000.00");
		when(cardRepository.findAll()).thenReturn(List.of(card));
		when(marketPriceJobRepository.findDeliveredCardSalesBetween(any(), any())).thenReturn(List.of());
		when(priceHistoryRepository.findLatestAdminPrices(Set.of(CARD_ID)))
				.thenReturn(List.of(new AdminPriceRow(CARD_ID, new BigDecimal("1040.00"))));
		when(priceHistoryRepository.findForCardsByRecordedDate(TODAY, Set.of(CARD_ID))).thenReturn(List.of());

		MarketPriceJobSummary summary = service.runNow();

		assertThat(summary).isEqualTo(new MarketPriceJobSummary(1, 1, 0));
		assertThat(card.getMarketPrice()).isEqualByComparingTo("1040.00");
		ArgumentCaptor<PriceHistory> historyCaptor = ArgumentCaptor.forClass(PriceHistory.class);
		verify(priceHistoryRepository).save(historyCaptor.capture());
		assertThat(historyCaptor.getValue().getMarketPrice()).isEqualByComparingTo("1040.00");
		assertThat(historyCaptor.getValue().getAvgPrice()).isNull();
		assertThat(historyCaptor.getValue().getSource()).isEqualTo("JOB");
		verify(eventPublisher, never()).publishEvent(any(PriceUpdatedEvent.class));
	}

	@Test
	void runNow_doesNotOverwriteAdminEntryForToday() {
		Card card = card("1250.00");
		PriceHistory adminHistory = new PriceHistory();
		adminHistory.setCard(card);
		adminHistory.setRecordedDate(TODAY);
		adminHistory.setSource("ADMIN");
		when(cardRepository.findAll()).thenReturn(List.of(card));
		when(marketPriceJobRepository.findDeliveredCardSalesBetween(any(), any())).thenReturn(List.of());
		when(priceHistoryRepository.findLatestAdminPrices(Set.of(CARD_ID))).thenReturn(List.of());
		when(priceHistoryRepository.findForCardsByRecordedDate(TODAY, Set.of(CARD_ID)))
				.thenReturn(List.of(adminHistory));

		MarketPriceJobSummary summary = service.runNow();

		assertThat(summary).isEqualTo(new MarketPriceJobSummary(1, 0, 0));
		assertThat(card.getMarketPrice()).isEqualByComparingTo("1250.00");
		verify(priceHistoryRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any(PriceUpdatedEvent.class));
	}

	@Test
	void runNow_updatesButDoesNotPublishChangeBelowFivePercent() {
		Card card = card("1000.00");
		when(cardRepository.findAll()).thenReturn(List.of(card));
		when(marketPriceJobRepository.findDeliveredCardSalesBetween(any(), any()))
				.thenReturn(List.of(sales("1040.00", 1)));
		when(priceHistoryRepository.findLatestAdminPrices(Set.of(CARD_ID))).thenReturn(List.of());
		when(priceHistoryRepository.findForCardsByRecordedDate(TODAY, Set.of(CARD_ID))).thenReturn(List.of());

		MarketPriceJobSummary summary = service.runNow();

		assertThat(summary).isEqualTo(new MarketPriceJobSummary(1, 1, 0));
		assertThat(card.getMarketPrice()).isEqualByComparingTo("1040.00");
		verify(eventPublisher, never()).publishEvent(any(PriceUpdatedEvent.class));
	}

	private Card card(String marketPrice) {
		Card card = new Card();
		card.setId(CARD_ID);
		card.setName("Pikachu");
		card.setCardNumber("025");
		card.setRarity(Rarity.RARE);
		card.setLanguage(CardLanguage.ENGLISH);
		card.setMarketPrice(new BigDecimal(marketPrice));
		return card;
	}

	private DeliveredCardSales sales(String amount, long quantity) {
		return new DeliveredCardSalesRow(CARD_ID, new BigDecimal(amount), quantity);
	}

	private record DeliveredCardSalesRow(Long cardId, BigDecimal salesAmount, Long quantity)
			implements DeliveredCardSales {
		@Override
		public Long getCardId() {
			return cardId;
		}

		@Override
		public BigDecimal getSalesAmount() {
			return salesAmount;
		}

		@Override
		public Long getQuantity() {
			return quantity;
		}
	}

	private record AdminPriceRow(Long cardId, BigDecimal marketPrice) implements AdminMarketPrice {
		@Override
		public Long getCardId() {
			return cardId;
		}

		@Override
		public BigDecimal getMarketPrice() {
			return marketPrice;
		}
	}
}
