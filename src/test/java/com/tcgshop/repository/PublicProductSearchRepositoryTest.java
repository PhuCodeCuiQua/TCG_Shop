package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
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
class PublicProductSearchRepositoryTest {

	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private GameRepository gameRepository;
	@Autowired private SeriesRepository seriesRepository;
	@Autowired private CardRepository cardRepository;

	@Test
	void combinedFiltersReturnOneSingleCardWithMinimumPriceAndDistinctSellerCount() {
		Series series = saveSeries("Prismatic Horizons", "ph");
		Shop firstShop = saveShop("First Shop", ShopStatus.APPROVED);
		Shop secondShop = saveShop("Second Shop", ShopStatus.APPROVED);
		Shop pendingShop = saveShop("Pending Shop", ShopStatus.PENDING);
		Card card = saveCard(series, "Radiant Vanguard", "PH-001", Rarity.ULTRA_RARE);
		Product representative = saveProduct(firstShop, "Vanguard near mint", ProductType.SINGLE, card, null,
				new BigDecimal("25000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 3, 4, 12);
		saveProduct(secondShop, "Vanguard lightly played", ProductType.SINGLE, card, null,
				new BigDecimal("20000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 4, 8, 16);
		saveProduct(pendingShop, "Hidden offer", ProductType.SINGLE, card, null,
				new BigDecimal("19000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 99, 99, 99);
		Product banned = saveProduct(firstShop, "Banned low offer", ProductType.SINGLE, card, null,
				new BigDecimal("18000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 99, 99, 99);
		banned.setStatus(ProductStatus.BANNED);
		Product deleted = saveProduct(firstShop, "Deleted low offer", ProductType.SINGLE, card, null,
				new BigDecimal("17000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 99, 99, 99);
		deleted.setDeleted(true);
		productRepository.flush();

		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setKeyword("PH-001");
		filters.setGameId(series.getGame().getId());
		filters.setSeriesId(series.getId());
		filters.setRarity(Rarity.ULTRA_RARE);
		filters.setType(ProductType.SINGLE);
		filters.setLanguage(CardLanguage.ENGLISH);
		filters.setCondition(CardCondition.NEAR_MINT);
		filters.setPriceFrom(new BigDecimal("15000"));
		filters.setPriceTo(new BigDecimal("30000"));

		Page<PublicProductSearchProjection> results = productRepository.searchPublic(
				PublicProductSpecifications.from(filters),
				PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.PRICE_ASC,
				false);

		assertThat(results.getTotalElements()).isEqualTo(1);
		assertThat(results.getContent()).hasSize(1);
		assertThat(results.getContent().getFirst().singleCard()).isTrue();
		assertThat(results.getContent().getFirst().productId()).isEqualTo(representative.getId());
		assertThat(results.getContent().getFirst().name()).isEqualTo("Radiant Vanguard");
		assertThat(results.getContent().getFirst().cardNumber()).isEqualTo("PH-001");
		assertThat(results.getContent().getFirst().price()).isEqualByComparingTo("20000");
		assertThat(results.getContent().getFirst().sellerCount()).isEqualTo(2);
	}

	@Test
	void partialCaseInsensitiveKeywordAndShopFilterAreAppliedTogether() {
		Series series = saveSeries("Prismatic Horizons", "ph");
		Shop firstShop = saveShop("First Shop", ShopStatus.APPROVED);
		Shop secondShop = saveShop("Second Shop", ShopStatus.APPROVED);
		Card card = saveCard(series, "Radiant Vanguard", "PH-002", Rarity.ULTRA_RARE);
		saveProduct(firstShop, "First offer", ProductType.SINGLE, card, null,
				new BigDecimal("25000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 0, 0, 0);
		saveProduct(secondShop, "Second offer", ProductType.SINGLE, card, null,
				new BigDecimal("20000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 0, 0, 0);
		productRepository.flush();
		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setKeyword("DIANT VANG");
		filters.setShopId(firstShop.getId());

		Page<PublicProductSearchProjection> results = productRepository.searchPublic(
				PublicProductSpecifications.from(filters), PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.PRICE_ASC, false);

		assertThat(results.getTotalElements()).isEqualTo(1);
		assertThat(results.getContent().getFirst().name()).isEqualTo("Radiant Vanguard");
		assertThat(results.getContent().getFirst().price()).isEqualByComparingTo("25000");
		assertThat(results.getContent().getFirst().sellerCount()).isEqualTo(1);
	}

	@Test
	void topRatedTabRequiresAtLeastFiveReviewsAndCapsResultsToOneTopPage() {
		Series series = saveSeries("Rare Collection", "rare");
		Shop shop = saveShop("Rated Shop", ShopStatus.APPROVED);
		Card eligible = saveCard(series, "Eligible Card", "R-01", Rarity.RARE);
		Card belowThreshold = saveCard(series, "Few Reviews", "R-02", Rarity.RARE);
		saveProduct(shop, "Eligible offer", ProductType.SINGLE, eligible, null,
				new BigDecimal("10000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 6, 5, 10);
		saveProduct(shop, "Unrated offer", ProductType.SINGLE, belowThreshold, null,
				new BigDecimal("5000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 5, 4, 20);
		productRepository.flush();

		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setSort(PublicProductSearchRequest.SortBy.TOP_RATED);

		Page<PublicProductSearchProjection> results = productRepository.searchPublic(
				PublicProductSpecifications.from(filters),
				PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.TOP_RATED,
				true);

		assertThat(results.getContent()).extracting(PublicProductSearchProjection::name)
				.containsExactly("Eligible Card");
	}

	@Test
	void bestSellingAndFavoriteTabsRankAggregatedSingleCardMetrics() {
		Series series = saveSeries("Collector Series", "collector");
		Shop firstShop = saveShop("Metric Shop One", ShopStatus.APPROVED);
		Shop secondShop = saveShop("Metric Shop Two", ShopStatus.APPROVED);
		Card mostSold = saveCard(series, "Most Sold", "C-01", Rarity.RARE);
		Card mostFavorited = saveCard(series, "Most Favorited", "C-02", Rarity.RARE);
		saveProduct(firstShop, "Most Sold One", ProductType.SINGLE, mostSold, null,
				new BigDecimal("12000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 30, 1, 1);
		saveProduct(secondShop, "Most Sold Two", ProductType.SINGLE, mostSold, null,
				new BigDecimal("11000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 25, 1, 1);
		saveProduct(firstShop, "Most Favorited", ProductType.SINGLE, mostFavorited, null,
				new BigDecimal("9000"), CardLanguage.ENGLISH, CardCondition.NEAR_MINT, 4, 1, 20);
		productRepository.flush();
		PublicProductSearchRequest filters = new PublicProductSearchRequest();

		Page<PublicProductSearchProjection> bestSelling = productRepository.searchPublic(
				PublicProductSpecifications.from(filters), PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.BEST_SELLING, true);
		Page<PublicProductSearchProjection> favorites = productRepository.searchPublic(
				PublicProductSpecifications.from(filters), PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.FAVORITES, true);

		assertThat(bestSelling.getContent().getFirst().name()).isEqualTo("Most Sold");
		assertThat(favorites.getContent().getFirst().name()).isEqualTo("Most Favorited");
	}

	@Test
	void sealedProductsRemainIndividualCatalogResults() {
		Series series = saveSeries("Sealed Series", "sealed");
		Shop shop = saveShop("Sealed Shop", ShopStatus.APPROVED);
		Product sealed = new Product();
		sealed.setShop(shop);
		sealed.setName("Booster Box");
		sealed.setType(ProductType.BOOSTER_BOX);
		sealed.setSeries(series);
		sealed.setPrice(new BigDecimal("45000"));
		sealed.setStock(3);
		sealed.setSoldCount(7);
		sealed.setStatus(ProductStatus.ACTIVE);
		sealed.setDeleted(false);
		productRepository.saveAndFlush(sealed);

		PublicProductSearchRequest filters = new PublicProductSearchRequest();
		filters.setType(ProductType.BOOSTER_BOX);
		Page<PublicProductSearchProjection> results = productRepository.searchPublic(
				PublicProductSpecifications.from(filters), PageRequest.of(0, 20),
				PublicProductSearchRequest.SortBy.PRICE_ASC, false);

		assertThat(results.getTotalElements()).isEqualTo(1);
		assertThat(results.getContent().getFirst().singleCard()).isFalse();
		assertThat(results.getContent().getFirst().productId()).isEqualTo(sealed.getId());
		assertThat(results.getContent().getFirst().shopId()).isEqualTo(shop.getId());
		assertThat(results.getContent().getFirst().name()).isEqualTo("Booster Box");
		assertThat(results.getContent().getFirst().price()).isEqualByComparingTo("45000");
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
		return seriesRepository.save(series);
	}

	private Shop saveShop(String name, ShopStatus status) {
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
		shop.setDeleted(false);
		return shopRepository.save(shop);
	}

	private Card saveCard(Series series, String name, String number, Rarity rarity) {
		Card card = new Card();
		card.setSeries(series);
		card.setName(name);
		card.setCardNumber(number);
		card.setLanguage(CardLanguage.ENGLISH);
		card.setRarity(rarity);
		return cardRepository.save(card);
	}

	private Product saveProduct(
			Shop shop, String name, ProductType type, Card card, Series series,
			BigDecimal price, CardLanguage language, CardCondition condition,
			int sold, int reviews, int favorites) {
		Product product = new Product();
		product.setShop(shop);
		product.setName(name);
		product.setType(type);
		product.setCard(card);
		product.setSeries(series);
		product.setPrice(price);
		product.setStock(5);
		product.setSoldCount(sold);
		product.setAvgRating(reviews == 0 ? null : new BigDecimal("4.80"));
		product.setRatingCount(reviews);
		product.setFavoriteCount(favorites);
		product.setLanguage(language);
		product.setCardCondition(condition);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.save(product);
	}
}
