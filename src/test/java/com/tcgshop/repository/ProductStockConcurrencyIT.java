// Chạy bằng mvnw test -DrunIT=true; kiểm thử này dùng DB đang được cấu hình cho ứng dụng.
package com.tcgshop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@EnabledIfSystemProperty(named = "runIT", matches = "true")
class ProductStockConcurrencyIT {
	private static final int WORKER_COUNT = 20;

	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PlatformTransactionManager transactionManager;

	private Long userId;
	private Long shopId;
	private Long productId;

	@Test
	void atomicStockDecrementNeverOversellsAndAdvancesVersionPerSuccessfulUpdate() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "");
		User owner = new User();
		owner.setEmail("it-" + suffix + "@example.test");
		owner.setFullName("Integration owner");
		owner.setPassword("not-used-in-test");
		owner.setStatus(UserStatus.ACTIVE);
		owner = userRepository.saveAndFlush(owner);
		userId = owner.getId();

		Shop shop = new Shop();
		shop.setOwner(owner);
		shop.setName("it-shop-" + suffix);
		shop.setSlug("it-shop-" + suffix);
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		shop = shopRepository.saveAndFlush(shop);
		shopId = shop.getId();

		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName("it-product-" + suffix);
		product.setPrice(new BigDecimal("1000"));
		product.setStock(5);
		product.setSoldCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product = productRepository.saveAndFlush(product);
		productId = product.getId();
		Long initialVersion = product.getVersion();
		assertNotNull(initialVersion);

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		CountDownLatch ready = new CountDownLatch(WORKER_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
		try {
			List<Future<Integer>> attempts = new ArrayList<>(WORKER_COUNT);
			for (int index = 0; index < WORKER_COUNT; index++) {
				attempts.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(15, TimeUnit.SECONDS)) {
						throw new IllegalStateException("Concurrent workers did not receive start signal");
					}
					return transactionTemplate.execute(status ->
							productRepository.decreaseStock(productId, 1, ProductStatus.ACTIVE));
				}));
			}
			assertTrue(ready.await(15, TimeUnit.SECONDS), "All workers should be ready");
			start.countDown();
			List<Integer> results = new ArrayList<>(WORKER_COUNT);
			for (Future<Integer> attempt : attempts) {
				results.add(attempt.get(30, TimeUnit.SECONDS));
			}
			assertEquals(5, results.stream().filter(rows -> rows == 1).count());
			assertEquals(15, results.stream().filter(rows -> rows == 0).count());
		} finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		}

		Product updated = productRepository.findById(productId).orElseThrow();
		assertEquals(0, updated.getStock());
		assertEquals(initialVersion + 5, updated.getVersion());
	}

	@AfterEach
	void cleanUpIntegrationData() {
		if (productId != null) {
			productRepository.deleteById(productId);
		}
		if (shopId != null) {
			shopRepository.deleteById(shopId);
		}
		if (userId != null) {
			userRepository.deleteById(userId);
		}
	}
}
