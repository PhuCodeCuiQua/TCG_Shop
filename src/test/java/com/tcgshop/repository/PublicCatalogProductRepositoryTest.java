package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class PublicCatalogProductRepositoryTest {

	@Autowired private PublicCatalogProductRepository publicCatalogProductRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private GameRepository gameRepository;
	@Autowired private SeriesRepository seriesRepository;
	@Autowired private CardRepository cardRepository;

	@Test
	void seriesListingsIncludeApprovedSinglesAndSealedProductsWithPagination() {
		Series series = saveSeries("Visible Series", "visible");
		Shop approvedShop = saveShop("Approved Shop", ShopStatus.APPROVED, false);
		Shop pendingShop = saveShop("Pending Shop", ShopStatus.PENDING, false);
		Card card = saveCard(series, "Pikachu", "P-01", true);

		saveProduct(approvedShop, "Pikachu Single", ProductType.SINGLE, card, null,
				ProductStatus.ACTIVE, false);
		saveProduct(approvedShop, "Visible Booster Box", ProductType.BOOSTER_BOX, null, series,
				ProductStatus.ACTIVE, false);
		saveProduct(approvedShop, "Banned Product", ProductType.SINGLE, card, null,
				ProductStatus.BANNED, false);
		saveProduct(approvedShop, "Deleted Product", ProductType.SINGLE, card, null,
				ProductStatus.ACTIVE, true);
		saveProduct(pendingShop, "Pending Shop Product", ProductType.SINGLE, card, null,
				ProductStatus.ACTIVE, false);
		productRepository.flush();

		Page<PublicCatalogProductProjection> results =
				publicCatalogProductRepository.findPublicProductsBySeriesId(series.getId(), PageRequest.of(0, 1));

		assertThat(results.getTotalElements()).isEqualTo(2);
		assertThat(results.getTotalPages()).isEqualTo(2);
		assertThat(results.getContent()).hasSize(1);
		assertThat(results.getContent().getFirst().getName()).isEqualTo("Pikachu Single");

		Page<PublicCatalogProductProjection> nextPage =
				publicCatalogProductRepository.findPublicProductsBySeriesId(series.getId(), PageRequest.of(1, 1));
		assertThat(nextPage.getContent()).extracting(PublicCatalogProductProjection::getName)
				.containsExactly("Visible Booster Box");
	}

	@Test
	void hiddenCardsAndHiddenSeriesAreNotShownInPublicListings() {
		Series series = saveSeries("Active Series", "active");
		Shop shop = saveShop("Open Shop", ShopStatus.APPROVED, false);
		Card hiddenCard = saveCard(series, "Hidden Card", "H-01", false);
		saveProduct(shop, "Hidden Card Listing", ProductType.SINGLE, hiddenCard, null,
				ProductStatus.ACTIVE, false);

		Series hiddenSeries = saveSeries("Hidden Series", "hidden");
		hiddenSeries.setActive(false);
		seriesRepository.saveAndFlush(hiddenSeries);
		saveProduct(shop, "Hidden Series Sealed", ProductType.BOOSTER_PACK, null, hiddenSeries,
				ProductStatus.ACTIVE, false);
		productRepository.flush();

		assertThat(publicCatalogProductRepository.findPublicProductsBySeriesId(
				series.getId(), PageRequest.of(0, 10)).getContent()).isEmpty();
		assertThat(publicCatalogProductRepository.findPublicProductsBySeriesId(
				hiddenSeries.getId(), PageRequest.of(0, 10)).getContent()).isEmpty();
	}

	private Series saveSeries(String name, String code) {
		Game game = new Game();
		game.setName("Game " + code);
		game.setCode(code.toUpperCase());
		gameRepository.save(game);

		Series series = new Series();
		series.setGame(game);
		series.setName(name);
		series.setCode(code);
		series.setReleaseDate(LocalDate.of(2026, 1, 1));
		series.setActive(true);
		return seriesRepository.save(series);
	}

	private Shop saveShop(String name, ShopStatus status, boolean deleted) {
		User owner = new User();
		owner.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
		owner.setFullName(name + " Owner");
		owner.setPassword("encoded-password");
		owner.setStatus(UserStatus.ACTIVE);
		userRepository.save(owner);

		Shop shop = new Shop();
		shop.setOwner(owner);
		shop.setName(name);
		shop.setSlug(name.toLowerCase().replace(' ', '-'));
		shop.setStatus(status);
		shop.setDeleted(deleted);
		return shopRepository.save(shop);
	}

	private Card saveCard(Series series, String name, String cardNumber, boolean active) {
		Card card = new Card();
		card.setSeries(series);
		card.setName(name);
		card.setCardNumber(cardNumber);
		card.setLanguage(CardLanguage.ENGLISH);
		card.setRarity(Rarity.RARE);
		card.setActive(active);
		return cardRepository.save(card);
	}

	private void saveProduct(
			Shop shop, String name, ProductType type, Card card, Series series,
			ProductStatus status, boolean deleted) {
		Product product = new Product();
		product.setShop(shop);
		product.setName(name);
		product.setType(type);
		product.setCard(card);
		product.setSeries(series);
		product.setPrice(new BigDecimal("1000.00"));
		product.setStock(5);
		product.setSoldCount(0);
		product.setStatus(status);
		product.setDeleted(deleted);
		productRepository.save(product);
	}
}
