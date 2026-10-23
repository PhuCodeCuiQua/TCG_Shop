package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.UserStatus;
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
class ProductRepositoryTest {

	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;

	@Test
	void searchFiltersByShopTypeAndStatusAndExcludesSoftDeletedProducts() {
		Shop firstShop = saveShop("Card Store");
		Shop secondShop = saveShop("Other Store");
		saveProduct(firstShop, "Vi phạm chính sách", ProductType.SINGLE, ProductStatus.BANNED, false);
		saveProduct(firstShop, "Sản phẩm đang bán", ProductType.SINGLE, ProductStatus.ACTIVE, false);
		saveProduct(secondShop, "Vi phạm khác", ProductType.SINGLE, ProductStatus.BANNED, false);
		saveProduct(firstShop, "Đã xóa mềm", ProductType.SINGLE, ProductStatus.BANNED, true);

		Page<Product> result = productRepository.searchForModeration(
				firstShop.getId(), ProductType.SINGLE, ProductStatus.BANNED, "vi phạm", PageRequest.of(0, 10));

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).extracting(Product::getName).containsExactly("Vi phạm chính sách");
	}

	@Test
	void searchMatchesShopNameAndReturnsPaginatedResults() {
		Shop shop = saveShop("Rare Cards");
		saveProduct(shop, "Gói thẻ số một", ProductType.BOOSTER_PACK, ProductStatus.ACTIVE, false);
		saveProduct(shop, "Gói thẻ số hai", ProductType.BOOSTER_PACK, ProductStatus.ACTIVE, false);

		Page<Product> result = productRepository.searchForModeration(
				null, null, null, "rare cards", PageRequest.of(0, 1));

		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getTotalPages()).isEqualTo(2);
	}

	private Shop saveShop(String name) {
		User owner = new User();
		owner.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
		owner.setFullName(name + " Owner");
		owner.setPassword("encoded-password");
		owner.setStatus(UserStatus.ACTIVE);
		userRepository.save(owner);

		Shop shop = new Shop();
		shop.setName(name);
		shop.setSlug(name.toLowerCase().replace(' ', '-'));
		shop.setOwner(owner);
		shop.setDeleted(false);
		return shopRepository.save(shop);
	}

	private Product saveProduct(
			Shop shop, String name, ProductType type, ProductStatus status, boolean deleted) {
		Product product = new Product();
		product.setShop(shop);
		product.setName(name);
		product.setType(type);
		product.setStatus(status);
		product.setDeleted(deleted);
		return productRepository.save(product);
	}
}
