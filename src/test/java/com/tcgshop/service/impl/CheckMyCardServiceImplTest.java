package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.CardOfferResponse;
import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.dto.response.CheckMyCardResponse;
import com.tcgshop.dto.response.PriceChartResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.service.CardOfferService;
import com.tcgshop.service.PriceChartService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckMyCardServiceImplTest {
	private static final Long CARD_ID = 7L;

	@Mock
	private CardRepository cardRepository;
	@Mock
	private CardOfferService cardOfferService;
	@Mock
	private PriceChartService priceChartService;
	@InjectMocks
	private CheckMyCardServiceImpl service;

	@Test
	void check_filtersLanguageAndComputesCheapestSellerCountAndDifference() {
		Card card = activeCard(new BigDecimal("1000.00"));
		when(cardRepository.findByIdAndActiveTrue(CARD_ID)).thenReturn(Optional.of(card));
		when(cardOfferService.getOffers(CARD_ID, CardCondition.NEAR_MINT, "price", "asc"))
				.thenReturn(new CardOffersResponse(List.of(
						offer(101L, 10L, "ENGLISH", "800.00"),
						offer(102L, 10L, "ENGLISH", "900.00"),
						offer(103L, 11L, "JAPANESE", "500.00")), List.of()));
		when(priceChartService.getChart(CARD_ID, 30)).thenReturn(emptyChart());

		CheckMyCardResponse result = service.check(
				CARD_ID, CardCondition.NEAR_MINT, CardLanguage.ENGLISH);

		assertThat(result.lowestSellingPrice()).isEqualByComparingTo("800.00");
		assertThat(result.sellerCount()).isEqualTo(1);
		assertThat(result.priceDifferencePercent()).isEqualByComparingTo("-20.00");
		assertThat(result.cheapestProductId()).isEqualTo(101L);
		assertThat(result.chartPoints()).isEmpty();
		verify(cardOfferService).getOffers(CARD_ID, CardCondition.NEAR_MINT, "price", "asc");
	}

	@Test
	void check_withoutMarketPriceOrOffersReturnsClearEmptyData() {
		when(cardRepository.findByIdAndActiveTrue(CARD_ID)).thenReturn(Optional.of(activeCard(null)));
		when(cardOfferService.getOffers(CARD_ID, null, "price", "asc"))
				.thenReturn(new CardOffersResponse(List.of(), List.of()));
		when(priceChartService.getChart(CARD_ID, 30)).thenReturn(emptyChart());

		CheckMyCardResponse result = service.check(CARD_ID, null, null);

		assertThat(result.marketPrice()).isNull();
		assertThat(result.lowestSellingPrice()).isNull();
		assertThat(result.sellerCount()).isZero();
		assertThat(result.priceDifferencePercent()).isNull();
		assertThat(result.cheapestProductId()).isNull();
		assertThat(result.chartPoints()).isEmpty();
	}

	@Test
	void check_inactiveOrMissingCardIsNotPubliclyAvailable() {
		when(cardRepository.findByIdAndActiveTrue(CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.check(CARD_ID, null, null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Card activeCard(BigDecimal marketPrice) {
		Game game = new Game();
		game.setActive(true);
		Series series = new Series();
		series.setActive(true);
		series.setGame(game);
		Card card = new Card();
		card.setId(CARD_ID);
		card.setName("Pikachu");
		card.setCardNumber("025");
		card.setMarketPrice(marketPrice);
		card.setSeries(series);
		return card;
	}

	private CardOfferResponse offer(Long productId, Long shopId, String language, String effectivePrice) {
		return new CardOfferResponse(
				productId, shopId, "Shop " + shopId, null, 0, "NEAR_MINT", language,
				new BigDecimal(effectivePrice), new BigDecimal(effectivePrice), 5);
	}

	private PriceChartResponse emptyChart() {
		return new PriceChartResponse(CARD_ID, 30, null, null, List.of(), null, null, null, false);
	}
}
