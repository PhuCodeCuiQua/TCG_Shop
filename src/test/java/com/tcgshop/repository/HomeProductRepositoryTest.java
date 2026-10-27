package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.ViewedProduct;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class HomeProductRepositoryTest {

	@Autowired private HomeProductRepository homeProductRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private ViewedProductRepository viewedProductRepository;

	@Test
	void suppliesQueryIncludesAllFourTypesAndExcludesUnavailableProducts() {
		Shop shop = saveShop("Supplies Shop", ShopStatus.APPROVED, false);
		Shop pending = saveShop("Pending Supplies", ShopStatus.PENDING, false);
		Shop deleted = saveShop("Deleted Supplies", ShopStatus.APPROVED, true);
		for (ProductType type : List.of(ProductType.SUPPLY, ProductType.BOOSTER_BOX, ProductType.BOOSTER_PACK, ProductType.BUNDLE)) {
			Product p = saveProduct(shop, type.name(), 0, ProductStatus.ACTIVE, false);
			p.setType(type);
			p.setStock(5);
		}
		Product single = saveProduct(shop, "Single", 0, ProductStatus.ACTIVE, false);
		single.setType(ProductType.SINGLE);
		Product noStock = saveProduct(shop, "No stock", 0, ProductStatus.ACTIVE, false);
		noStock.setType(ProductType.SUPPLY); noStock.setStock(0);
		for (Product p : List.of(saveProduct(shop, "Banned", 0, ProductStatus.BANNED, false),
				saveProduct(shop, "Deleted", 0, ProductStatus.ACTIVE, true),
				saveProduct(pending, "Pending", 0, ProductStatus.ACTIVE, false),
				saveProduct(deleted, "Deleted shop", 0, ProductStatus.ACTIVE, false))) {
			p.setType(ProductType.SUPPLY); p.setStock(5);
		}
		productRepository.flush();
		assertThat(homeProductRepository.findSuppliesAndSealed(PageRequest.of(0, 8)))
				.extracting(HomeProductProjection::getName)
				.containsExactlyInAnyOrder("SUPPLY", "BOOSTER_BOX", "BOOSTER_PACK", "BUNDLE");
		for (int i = 0; i < 9; i++) {
			Product p = saveProduct(shop, "Extra " + i, 0, ProductStatus.ACTIVE, false);
			p.setType(ProductType.SUPPLY); p.setStock(2);
		}
		productRepository.flush();
		assertThat(homeProductRepository.findSuppliesAndSealed(PageRequest.of(0, 8))).hasSize(8);
	}

	@Test
	void bestsellerQueryExcludesTenOrFewerSalesAndOrdersBySalesDescending() {
		Shop approvedShop = saveShop("Approved Shop", ShopStatus.APPROVED, false);
		Shop pendingShop = saveShop("Pending Shop", ShopStatus.PENDING, false);
		saveProduct(approvedShop, "Twenty sales", 20, ProductStatus.ACTIVE, false);
		saveProduct(approvedShop, "Eleven sales", 11, ProductStatus.ACTIVE, false);
		saveProduct(approvedShop, "Ten sales", 10, ProductStatus.ACTIVE, false);
		saveProduct(approvedShop, "Banned product", 30, ProductStatus.BANNED, false);
		saveProduct(approvedShop, "Deleted product", 30, ProductStatus.ACTIVE, true);
		saveProduct(pendingShop, "Pending shop product", 30, ProductStatus.ACTIVE, false);
		productRepository.flush();

		List<HomeProductProjection> results =
				homeProductRepository.findBestSellers(PageRequest.of(0, 20));

		assertThat(results).extracting(HomeProductProjection::getName)
				.containsExactly("Twenty sales", "Eleven sales");
		assertThat(results).allSatisfy(product -> assertThat(product.getSoldCount()).isGreaterThan(10));
	}

	@Test
	void recentlyViewedQueryIsScopedToTheUserAndNewestFirst() {
		Shop shop = saveShop("History Shop", ShopStatus.APPROVED, false);
		Product firstProduct = saveProduct(shop, "First viewed", 14, ProductStatus.ACTIVE, false);
		Product secondProduct = saveProduct(shop, "Second viewed", 12, ProductStatus.ACTIVE, false);
		User firstUser = saveUser("first@example.com");
		User otherUser = saveUser("other@example.com");
		saveView(firstUser, firstProduct, LocalDateTime.of(2026, 1, 1, 10, 0));
		saveView(firstUser, secondProduct, LocalDateTime.of(2026, 1, 2, 10, 0));
		saveView(otherUser, firstProduct, LocalDateTime.of(2026, 1, 3, 10, 0));

		List<HomeProductProjection> results =
				homeProductRepository.findRecentlyViewedByUserId(firstUser.getId(), PageRequest.of(0, 8));

		assertThat(results).extracting(HomeProductProjection::getName)
				.containsExactly("Second viewed", "First viewed");
	}

	@Test
	void recommendationQueryRequiresMoreThanTenSalesAndOrdersByRating() {
		Shop shop = saveShop("Ratings Shop", ShopStatus.APPROVED, false);
		saveRatedProduct(shop, "Four point five", 30, "4.50", 20);
		saveRatedProduct(shop, "Five stars", 15, "5.00", 20);
		saveRatedProduct(shop, "Only ten sales", 10, "5.00", 50);
		productRepository.flush();

		List<HomeProductProjection> results =
				homeProductRepository.findRecommendedProducts(PageRequest.of(0, 10));

		assertThat(results).extracting(HomeProductProjection::getName)
				.containsExactly("Five stars", "Four point five");
		assertThat(results).allSatisfy(product -> assertThat(product.getSoldCount()).isGreaterThan(10));
	}

	private Shop saveShop(String name, ShopStatus status, boolean deleted) {
		User owner = saveUser(name.toLowerCase().replace(' ', '.') + "@example.com");
		Shop shop = new Shop();
		shop.setOwner(owner);
		shop.setName(name);
		shop.setSlug(name.toLowerCase().replace(' ', '-'));
		shop.setStatus(status);
		shop.setDeleted(deleted);
		return shopRepository.save(shop);
	}

	private User saveUser(String email) {
		User user = new User();
		user.setEmail(email);
		user.setFullName("Test Owner");
		user.setPassword("encoded-password");
		user.setStatus(UserStatus.ACTIVE);
		return userRepository.save(user);
	}

	private Product saveProduct(
			Shop shop, String name, int soldCount, ProductStatus status, boolean deleted) {
		Product product = new Product();
		product.setShop(shop);
		product.setName(name);
		product.setType(ProductType.SINGLE);
		product.setPrice(java.math.BigDecimal.valueOf(1000));
		product.setStock(2);
		product.setSoldCount(soldCount);
		product.setStatus(status);
		product.setDeleted(deleted);
		return productRepository.save(product);
	}

	private Product saveRatedProduct(Shop shop, String name, int soldCount, String rating, int ratingCount) {
		Product product = saveProduct(shop, name, soldCount, ProductStatus.ACTIVE, false);
		product.setAvgRating(new BigDecimal(rating));
		product.setRatingCount(ratingCount);
		return productRepository.save(product);
	}

	private void saveView(User user, Product product, LocalDateTime viewedAt) {
		ViewedProduct viewedProduct = new ViewedProduct();
		viewedProduct.setUser(user);
		viewedProduct.setProduct(product);
		viewedProduct.setViewedAt(viewedAt);
		viewedProductRepository.save(viewedProduct);
	}
}
