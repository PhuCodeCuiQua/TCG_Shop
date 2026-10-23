package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PriceHistoryRepositoryTest {

	@Autowired private PriceHistoryRepository priceHistoryRepository;
	@Autowired private CardRepository cardRepository;
	@Autowired private SeriesRepository seriesRepository;
	@Autowired private GameRepository gameRepository;

	@Test
	void searchFiltersCardAndInclusiveDateRangeAndPaginates() {
		Card firstCard = saveCard("Pikachu", "SV1");
		Card secondCard = saveCard("Eevee", "SV2");
		saveHistory(firstCard, LocalDate.of(2026, 10, 20), "100.00", "90.00");
		saveHistory(firstCard, LocalDate.of(2026, 10, 21), "110.00", "100.00");
		saveHistory(firstCard, LocalDate.of(2026, 10, 22), "120.00", "110.00");
		saveHistory(secondCard, LocalDate.of(2026, 10, 21), "200.00", "190.00");

		Page<PriceHistory> result = priceHistoryRepository.searchByCardAndDateRange(
				firstCard.getId(), LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 22),
				PageRequest.of(0, 1));

		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getCard().getName()).isEqualTo("Pikachu");
		assertThat(result.getTotalPages()).isEqualTo(2);
	}

	@Test
	void findsLatestHistoryByRecordedDate() {
		Card card = saveCard("Mew", "SV3");
		saveHistory(card, LocalDate.of(2026, 10, 22), "80.00", "75.00");
		saveHistory(card, LocalDate.of(2026, 10, 23), "90.00", "85.00");

		assertThat(priceHistoryRepository.findTopByCardIdOrderByRecordedDateDesc(card.getId()))
				.isPresent()
				.get()
				.extracting(PriceHistory::getMarketPrice)
				.isEqualTo(new BigDecimal("90.00"));
	}

	private Card saveCard(String name, String code) {
		Game game = new Game();
		game.setName("Game " + code);
		game.setCode(code);
		gameRepository.save(game);

		Series series = new Series();
		series.setGame(game);
		series.setName("Series " + code);
		series.setCode(code);
		seriesRepository.save(series);

		Card card = new Card();
		card.setSeries(series);
		card.setName(name);
		card.setCardNumber(code);
		card.setRarity(Rarity.RARE);
		card.setLanguage(CardLanguage.ENGLISH);
		return cardRepository.save(card);
	}

	private PriceHistory saveHistory(Card card, LocalDate date, String marketPrice, String avgPrice) {
		PriceHistory history = new PriceHistory();
		history.setCard(card);
		history.setRecordedDate(date);
		history.setMarketPrice(new BigDecimal(marketPrice));
		history.setAvgPrice(new BigDecimal(avgPrice));
		return priceHistoryRepository.save(history);
	}
}
