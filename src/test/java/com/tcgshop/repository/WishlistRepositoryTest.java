package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.Wishlist;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class WishlistRepositoryTest {
	@Autowired private WishlistRepository repository;
	@Autowired private EntityManager entityManager;
	private User buyer;
	private Shop shop;
	private Series series;
	private Card card;

	@BeforeEach
	void setUp() {
		buyer = new User();
		buyer.setEmail("wishlist-buyer@example.com");
		entityManager.persist(buyer);
		shop = new Shop();
		shop.setName("Wishlist shop");
		shop.setSlug("wishlist-shop");
		shop.setOwner(buyer);
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		entityManager.persist(shop);
		Game game = new Game();
		game.setName("Wishlist game");
		game.setCode("WISH");
		entityManager.persist(game);
		series = new Series();
		series.setGame(game);
		series.setName("Wishlist series");
		series.setCode("W1");
		entityManager.persist(series);
		card = new Card();
		card.setSeries(series);
		card.setName("Wishlist card");
		card.setCardNumber("W1-001");
		card.setLanguage(CardLanguage.ENGLISH);
		card.setImageUrl("https://example.com/card.png");
		entityManager.persist(card);
	}

	@Test
	void optionalCardSeriesAndMainImageDoNotRemoveProductsOrBreakPageCount() {
		Product single = product("Single", ProductType.SINGLE, card, null);
		Product sealed = product("Sealed", ProductType.BOOSTER_BOX, null, series);
		Product supply = product("Supply without image", ProductType.SUPPLY, null, null);
		ProductImage image = new ProductImage();
		image.setProduct(sealed);
		image.setIsMain(true);
		image.setImageUrl("https://example.com/box.png");
		entityManager.persist(image);
		entityManager.flush();
		entityManager.clear();

		var page = repository.findVisibleProductsByUserId(buyer.getId(), PageRequest.of(0, 1));
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(3);
		var items = repository.findVisibleProductsByUserId(buyer.getId(), PageRequest.of(0, 12));
		assertThat(items.getContent()).extracting(WishlistProductProjection::getProductId)
				.containsExactlyInAnyOrder(single.getId(), sealed.getId(), supply.getId());
		assertThat(items.getContent()).filteredOn(item -> item.getProductId().equals(single.getId()))
				.extracting(WishlistProductProjection::getImageUrl).containsExactly("https://example.com/card.png");
		assertThat(items.getContent()).filteredOn(item -> item.getProductId().equals(sealed.getId()))
				.extracting(WishlistProductProjection::getImageUrl).containsExactly("https://example.com/box.png");
		assertThat(items.getContent()).filteredOn(item -> item.getProductId().equals(supply.getId()))
				.allSatisfy(item -> assertThat(item.getImageUrl()).isNull());
		assertThat(repository.findProductIdsByUserIdAndProductIdIn(buyer.getId(),
				List.of(single.getId(), sealed.getId(), supply.getId())))
				.containsExactlyInAnyOrder(single.getId(), sealed.getId(), supply.getId());
	}

	@Test
	void hiddenProductsAndInactiveCatalogStayExcluded() {
		Product visible = product("Visible supply", ProductType.SUPPLY, null, null);
		Product banned = product("Banned", ProductType.SUPPLY, null, null);
		banned.setStatus(ProductStatus.BANNED);
		Product deleted = product("Deleted", ProductType.SUPPLY, null, null);
		deleted.setDeleted(true);
		Product single = product("Hidden card", ProductType.SINGLE, card, null);
		Product sealed = product("Hidden series", ProductType.BOOSTER_BOX, null, series);
		series.setActive(false);
		entityManager.flush();

		assertThat(repository.findVisibleProductsByUserId(buyer.getId(), PageRequest.of(0, 12)).getContent())
				.extracting(WishlistProductProjection::getProductId).containsExactly(visible.getId());
		assertThat(repository.findProductIdsByUserIdAndProductIdIn(buyer.getId(),
				List.of(visible.getId(), banned.getId(), deleted.getId(), single.getId(), sealed.getId())))
				.containsExactly(visible.getId());
		shop.setStatus(ShopStatus.SUSPENDED);
		entityManager.flush();
		assertThat(repository.findVisibleProductsByUserId(buyer.getId(), PageRequest.of(0, 12))).isEmpty();
	}

	@Test
	void anotherUsersWishlistIsNotReturned() {
		Product product = product("Private wishlist", ProductType.SUPPLY, null, null);
		User other = new User();
		other.setEmail("other-wishlist@example.com");
		entityManager.persist(other);
		entityManager.flush();
		assertThat(repository.findVisibleProductsByUserId(other.getId(), PageRequest.of(0, 12))).isEmpty();
		assertThat(repository.findProductIdsByUserIdAndProductIdIn(other.getId(), List.of(product.getId())))
				.isEmpty();
	}

	private Product product(String name, ProductType type, Card linkedCard, Series linkedSeries) {
		Product product = new Product();
		product.setName(name);
		product.setType(type);
		product.setShop(shop);
		product.setCard(linkedCard);
		product.setSeries(linkedSeries);
		product.setPrice(new BigDecimal("25000"));
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		entityManager.persist(product);
		Wishlist wishlist = new Wishlist();
		wishlist.setUser(buyer);
		wishlist.setProduct(product);
		entityManager.persist(wishlist);
		return product;
	}
}
