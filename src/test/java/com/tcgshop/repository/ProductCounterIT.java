package com.tcgshop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
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
class ProductCounterIT {
	private static final int WORKER_COUNT = 20;

	@Autowired private ProductRepository productRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private OrderGroupRepository orderGroupRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderItemRepository orderItemRepository;
	@Autowired private PlatformTransactionManager transactionManager;

	private Long userId;
	private Long shopId;
	private Long firstProductId;
	private Long secondProductId;
	private Long orderGroupId;
	private Long orderId;

	@Test
	void updatesProductCountersAtomicallyAndAggregatesOrderItemQuantities() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "");
		User owner = new User();
		owner.setEmail("it-" + suffix + "@example.test");
		owner.setFullName("it-owner-" + suffix);
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

		Product firstProduct = saveProduct(shop, "it-product-a-" + suffix);
		firstProductId = firstProduct.getId();
		Product secondProduct = saveProduct(shop, "it-product-b-" + suffix);
		secondProductId = secondProduct.getId();
		Long initialVersion = firstProduct.getVersion();
		assertNotNull(initialVersion);

		assertEquals(1, productRepository.increaseStock(firstProductId, 2));
		Product afterStockIncrease = productRepository.findById(firstProductId).orElseThrow();
		assertEquals(7, afterStockIncrease.getStock());
		assertEquals(initialVersion + 1, afterStockIncrease.getVersion());

		assertEquals(1, productRepository.increaseSoldCount(firstProductId, 4));
		Product afterSoldIncrease = productRepository.findById(firstProductId).orElseThrow();
		assertEquals(7, afterSoldIncrease.getSoldCount());
		assertEquals(initialVersion + 1, afterSoldIncrease.getVersion());

		assertEquals(1, productRepository.decreaseSoldCount(firstProductId, 100));
		assertEquals(0, productRepository.findById(firstProductId).orElseThrow().getSoldCount());

		increaseSoldCountConcurrently(firstProductId);
		assertEquals(20, productRepository.findById(firstProductId).orElseThrow().getSoldCount());

		verifyGroupedOrderItemQuantities(owner, shop, firstProduct, secondProduct, suffix);
	}

	private Product saveProduct(Shop shop, String name) {
		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(name);
		product.setPrice(new BigDecimal("1000"));
		product.setStock(5);
		product.setSoldCount(3);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.saveAndFlush(product);
	}

	private void increaseSoldCountConcurrently(Long productId) throws Exception {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		CountDownLatch ready = new CountDownLatch(WORKER_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
		try {
			List<Future<Integer>> updates = new ArrayList<>(WORKER_COUNT);
			for (int index = 0; index < WORKER_COUNT; index++) {
				updates.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(15, TimeUnit.SECONDS)) {
						throw new IllegalStateException("Concurrent workers did not receive start signal");
					}
					return transactionTemplate.execute(status -> productRepository.increaseSoldCount(productId, 1));
				}));
			}
			assertTrue(ready.await(15, TimeUnit.SECONDS), "All workers should be ready");
			start.countDown();
			for (Future<Integer> update : updates) {
				assertEquals(1, update.get(30, TimeUnit.SECONDS));
			}
		} finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	private void verifyGroupedOrderItemQuantities(User owner, Shop shop, Product firstProduct,
			Product secondProduct, String suffix) {
		OrderGroup group = new OrderGroup();
		group.setGroupCode("it-group-" + suffix);
		group.setUser(owner);
		group.setTotalAmount(new BigDecimal("9000"));
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken("it-" + UUID.randomUUID());
		group = orderGroupRepository.saveAndFlush(group);
		orderGroupId = group.getId();

		Order order = new Order();
		order.setOrderCode("it-order-" + suffix);
		order.setOrderGroup(group);
		order.setUser(owner);
		order.setShop(shop);
		order.setStatus(OrderStatus.NEW);
		order.setSubtotal(new BigDecimal("9000"));
		order.setTotalAmount(new BigDecimal("9000"));
		order = orderRepository.saveAndFlush(order);
		orderId = order.getId();

		OrderItem firstLine = orderItem(order, firstProduct, 2);
		OrderItem secondLine = orderItem(order, secondProduct, 4);
		OrderItem repeatedFirstProductLine = orderItem(order, firstProduct, 3);
		orderItemRepository.saveAllAndFlush(List.of(firstLine, secondLine, repeatedFirstProductLine));

		List<OrderItemQuantity> quantities = orderItemRepository.findQuantitiesByOrderId(orderId);
		assertEquals(List.of(firstProduct.getId(), secondProduct.getId()),
				quantities.stream().map(OrderItemQuantity::getProductId).toList());
		assertEquals(List.of(5L, 4L), quantities.stream().map(OrderItemQuantity::getQuantity).toList());
	}

	private OrderItem orderItem(Order order, Product product, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setProductName(product.getName());
		item.setPrice(new BigDecimal("1000"));
		item.setQuantity(quantity);
		item.setTotalPrice(BigDecimal.valueOf(1000L * quantity));
		return item;
	}

	@AfterEach
	void cleanUpIntegrationData() {
		if (orderId != null) {
			orderRepository.deleteById(orderId);
		}
		if (orderGroupId != null) {
			orderGroupRepository.deleteById(orderGroupId);
		}
		if (firstProductId != null) {
			productRepository.deleteById(firstProductId);
		}
		if (secondProductId != null) {
			productRepository.deleteById(secondProductId);
		}
		if (shopId != null) {
			shopRepository.deleteById(shopId);
		}
		if (userId != null) {
			userRepository.deleteById(userId);
		}
	}
}
