package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MarketPriceJobRepositoryTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 10, 0);

	@Autowired
	private TestEntityManager entityManager;
	@Autowired
	private MarketPriceJobRepository marketPriceJobRepository;
	@Autowired
	private PriceHistoryRepository priceHistoryRepository;

	@Test
	void deliveredSalesQueryUsesOnlyRecentDeliveredSingleCardSnapshotsAndWeightsQuantity() {
		Card card = saveCard();
		Product product = saveProduct(card);
		Shop shop = saveShop();
		OrderGroup group = saveOrderGroup();
		saveOrderItem(saveOrder(group, shop, OrderStatus.DELIVERED, NOW.minusDays(2)), product, "100.00", 2);
		saveOrderItem(saveOrder(group, shop, OrderStatus.DELIVERED, NOW.minusDays(1)), product, "200.00", 3);
		saveOrderItem(saveOrder(group, shop, OrderStatus.CANCELLED, NOW.minusDays(1)), product, "900.00", 1);
		saveOrderItem(saveOrder(group, shop, OrderStatus.DELIVERED, NOW.minusDays(31)), product, "800.00", 1);
		entityManager.flush();

		List<DeliveredCardSales> result = marketPriceJobRepository.findDeliveredCardSalesBetween(
				NOW.minusDays(30), NOW);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getCardId()).isEqualTo(card.getId());
		assertThat(result.getFirst().getSalesAmount()).isEqualByComparingTo("800.00");
		assertThat(result.getFirst().getQuantity()).isEqualTo(5L);
	}

	@Test
	void priceHistoryQueriesReturnOneDailyRecordAndMostRecentlyUpdatedAdminValue() {
		Card card = saveCard();
		PriceHistory olderAdmin = saveHistory(card, LocalDate.of(2026, 10, 23), "900.00", "ADMIN",
				NOW.minusDays(2));
		PriceHistory latestAdmin = saveHistory(card, LocalDate.of(2026, 10, 24), "1100.00", "ADMIN",
				NOW.minusDays(1));
		saveHistory(card, LocalDate.of(2026, 10, 25), "1000.00", "JOB", NOW);
		entityManager.flush();
		entityManager.clear();

		List<PriceHistory> todaysRows = priceHistoryRepository.findForCardsByRecordedDate(
				LocalDate.of(2026, 10, 25), Set.of(card.getId()));
		List<AdminMarketPrice> adminRows = priceHistoryRepository.findLatestAdminPrices(Set.of(card.getId()));

		assertThat(todaysRows).hasSize(1);
		assertThat(todaysRows.getFirst().getSource()).isEqualTo("JOB");
		assertThat(adminRows).hasSize(1);
		assertThat(adminRows.getFirst().getMarketPrice()).isEqualByComparingTo("1100.00");
		assertThat(olderAdmin.getId()).isNotEqualTo(latestAdmin.getId());
	}

	private Card saveCard() {
		Game game = new Game();
		game.setName("Market Price Game");
		game.setCode("MPG");
		entityManager.persist(game);

		Series series = new Series();
		series.setGame(game);
		series.setName("Market Price Series");
		series.setCode("MPS");
		entityManager.persist(series);

		Card card = new Card();
		card.setSeries(series);
		card.setName("Market Price Card");
		card.setCardNumber("MP-001");
		card.setRarity(Rarity.RARE);
		card.setLanguage(CardLanguage.ENGLISH);
		entityManager.persist(card);
		return card;
	}

	private Product saveProduct(Card card) {
		Product product = new Product();
		product.setCard(card);
		product.setName("Market Price Product");
		product.setType(ProductType.SINGLE);
		product.setStatus(ProductStatus.ACTIVE);
		product.setPrice(new BigDecimal("100.00"));
		entityManager.persist(product);
		return product;
	}

	private Shop saveShop() {
		Shop shop = new Shop();
		shop.setName("Market Price Shop");
		shop.setSlug("market-price-shop");
		entityManager.persist(shop);
		return shop;
	}

	private OrderGroup saveOrderGroup() {
		OrderGroup group = new OrderGroup();
		group.setGroupCode("MPG-" + System.nanoTime());
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		entityManager.persist(group);
		return group;
	}

	private Order saveOrder(OrderGroup group, Shop shop, OrderStatus status, LocalDateTime deliveredAt) {
		Order order = new Order();
		order.setOrderCode("MP-" + System.nanoTime());
		order.setOrderGroup(group);
		order.setShop(shop);
		order.setStatus(status);
		order.setDeliveredAt(deliveredAt);
		entityManager.persist(order);
		return order;
	}

	private void saveOrderItem(Order order, Product product, String price, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setPrice(new BigDecimal(price));
		item.setQuantity(quantity);
		item.setTotalPrice(new BigDecimal(price).multiply(BigDecimal.valueOf(quantity)));
		entityManager.persist(item);
	}

	private PriceHistory saveHistory(Card card, LocalDate date, String price, String source,
			LocalDateTime updatedAt) {
		PriceHistory history = new PriceHistory();
		history.setCard(card);
		history.setRecordedDate(date);
		history.setMarketPrice(new BigDecimal(price));
		history.setSource(source);
		history.setUpdatedAt(updatedAt);
		entityManager.persist(history);
		return history;
	}
}
