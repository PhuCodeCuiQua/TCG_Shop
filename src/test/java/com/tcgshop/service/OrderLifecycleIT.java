package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.repository.OrderGroupRepository;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CustomUserDetails;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Chạy bằng mvnw test -DrunIT=true.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "runIT", matches = "true")
class OrderLifecycleIT {
	private static final BigDecimal FIRST_UNIT_PRICE = new BigDecimal("1000.00");
	private static final BigDecimal SECOND_UNIT_PRICE = new BigDecimal("2500.00");
	private static final String CANCELLATION_REASON = "it-cancel-reason";
	private static final String TRANSITION_REASON = "it-transition-reason";

	@Autowired private OrderService orderService;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderGroupRepository orderGroupRepository;
	@Autowired private OrderItemRepository orderItemRepository;
	@Autowired private OrderStatusHistoryRepository historyRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private CommissionService commissionService;
	@Autowired private JdbcTemplate jdbcTemplate;

	private User buyer;
	private User owner;
	private User shipper;
	private User manager;
	private Shop shop;
	private Product firstProduct;
	private Product secondProduct;
	private OrderGroup group;
	private Order order;
	private String prefix;
	private final List<Long> createdRoleIds = new ArrayList<>();

	@BeforeEach
	void createFixture() {
		prefix = "it-" + UUID.randomUUID().toString().replace("-", "");
		Role userRole = role(RoleName.ROLE_USER);
		Role vendorRole = role(RoleName.ROLE_VENDOR);
		Role shipperRole = role(RoleName.ROLE_SHIPPER);
		Role managerRole = role(RoleName.ROLE_MANAGER);

		buyer = saveUser("buyer", userRole);
		owner = saveUser("owner", userRole, vendorRole);
		shipper = saveUser("shipper", userRole, shipperRole);
		manager = saveUser("manager", managerRole);

		shop = new Shop();
		shop.setOwner(owner);
		shop.setName(prefix + "-shop");
		shop.setSlug(prefix + "-shop");
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		shop.setCommissionRate(new BigDecimal("5.00"));
		shop = shopRepository.saveAndFlush(shop);

		firstProduct = saveProduct("first", 8);
		secondProduct = saveProduct("second", 4);

		group = new OrderGroup();
		group.setGroupCode(prefix + "-group");
		group.setUser(buyer);
		group.setTotalAmount(new BigDecimal("6500.00"));
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken(UUID.randomUUID().toString());
		group = orderGroupRepository.saveAndFlush(group);

		order = new Order();
		order.setOrderCode(prefix + "-order");
		order.setOrderGroup(group);
		order.setUser(buyer);
		order.setShop(shop);
		order.setStatus(OrderStatus.NEW);
		order.setSubtotal(new BigDecimal("4500.00"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setPlatformDiscountAmount(BigDecimal.ZERO);
		order.setShippingFee(new BigDecimal("2000.00"));
		order.setTotalAmount(new BigDecimal("6500.00"));
		order = orderRepository.saveAndFlush(order);
		Long persistedOrderId = order.getId();

		OrderItem firstLine = orderItem(order, firstProduct, FIRST_UNIT_PRICE, 2);
		OrderItem secondLine = orderItem(order, secondProduct, SECOND_UNIT_PRICE, 1);
		orderItemRepository.saveAllAndFlush(List.of(firstLine, secondLine));
		OrderStatusHistory initialHistory = new OrderStatusHistory();
		initialHistory.setOrder(order);
		initialHistory.setPreviousStatus(null);
		initialHistory.setNewStatus(OrderStatus.NEW);
		initialHistory.setChangedBy(buyer);
		initialHistory.setNote(prefix + "-created");
		historyRepository.saveAndFlush(initialHistory);
		order = orderRepository.findById(persistedOrderId).orElseThrow();
	}

	@Test
	void completesDeliveryAndRecordsRevenueStockCountersAndHistory() {
		BigDecimal rate = commissionService.getRate(shop.getId());
		BigDecimal expectedCommission = new BigDecimal("4500.00").multiply(rate)
				.divide(new BigDecimal("100"), 0, java.math.RoundingMode.HALF_UP);

		actAs(owner);
		orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null);
		actAs(manager);
		orderService.assignShipper(order.getId(), shipper.getId());
		actAs(shipper);
		orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, null);
		orderService.changeStatus(order.getId(), OrderStatus.SHIPPING, null);
		orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, null);

		Order delivered = orderRepository.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.DELIVERED, delivered.getStatus());
		assertNotNull(delivered.getDeliveredAt());
		assertEquals(rate, delivered.getCommissionRate());
		assertEquals(expectedCommission, delivered.getCommissionAmount());
		assertEquals(new BigDecimal("4500.00").subtract(expectedCommission), delivered.getShopRevenue());
		assertTrue(delivered.isCodCollected());
		assertEquals(2, productRepository.findById(firstProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(1, productRepository.findById(secondProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(8, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(4, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
		assertHistory(List.of(
				new HistoryRow(null, OrderStatus.NEW, buyer.getId()),
				new HistoryRow(OrderStatus.NEW, OrderStatus.CONFIRMED, owner.getId()),
				new HistoryRow(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED, manager.getId()),
				new HistoryRow(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP, shipper.getId()),
				new HistoryRow(OrderStatus.PICKED_UP, OrderStatus.SHIPPING, shipper.getId()),
				new HistoryRow(OrderStatus.SHIPPING, OrderStatus.DELIVERED, shipper.getId())));
	}

	@Test
	void buyerCancellationRestocksOnceAndRecordsReason() {
		Long firstVersion = firstProduct.getVersion();
		Long secondVersion = secondProduct.getVersion();
		actAs(buyer);
		orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, CANCELLATION_REASON);

		Order cancelled = orderRepository.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
		assertEquals(CANCELLATION_REASON, cancelled.getCancelReason());
		assertEquals(10, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(5, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
		assertEquals(firstVersion + 1, productRepository.findById(firstProduct.getId()).orElseThrow().getVersion());
		assertEquals(secondVersion + 1, productRepository.findById(secondProduct.getId()).orElseThrow().getVersion());
		assertEquals(0, productRepository.findById(firstProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(0, productRepository.findById(secondProduct.getId()).orElseThrow().getSoldCount());

		assertThrows(BusinessException.class,
				() -> orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, CANCELLATION_REASON));
		assertEquals(10, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(5, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
	}

	@Test
	void rejectsWrongActorsWithoutSideEffects() {
		assertThrows(ForbiddenException.class,
				() -> as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null)));
		assertThrows(BusinessException.class,
				() -> as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, null)));
		assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(1, historyCount());
		assertStock(8, 4);

		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null));
		int historiesAfterValidSetup = historyCount();
		assertThrows(ForbiddenException.class,
				() -> as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, null)));
		assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(historiesAfterValidSetup, historyCount());
		assertStock(8, 4);
	}

	@Test
	void returnsDeliveredOrderAndRejectsRequestsOutsideWindow() {
		deliverOrder();
		LocalDateTime deliveredAt = orderRepository.findById(order.getId()).orElseThrow().getDeliveredAt();
		jdbcTemplate.update("update orders set delivered_at = ? where id = ?",
				Timestamp.valueOf(LocalDateTime.now().minusDays(8)), order.getId());
		assertThrows(BusinessException.class,
				() -> as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED,
						TRANSITION_REASON)));
		assertEquals(OrderStatus.DELIVERED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		jdbcTemplate.update("update orders set delivered_at = ? where id = ?", Timestamp.valueOf(deliveredAt), order.getId());

		BigDecimal commissionRate = orderRepository.findById(order.getId()).orElseThrow().getCommissionRate();
		BigDecimal commissionAmount = orderRepository.findById(order.getId()).orElseThrow().getCommissionAmount();
		BigDecimal shopRevenue = orderRepository.findById(order.getId()).orElseThrow().getShopRevenue();
		actAs(buyer);
		orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED, TRANSITION_REASON);
		actAs(owner);
		orderService.changeStatus(order.getId(), OrderStatus.RETURN_APPROVED, null);
		actAs(shipper);
		orderService.changeStatus(order.getId(), OrderStatus.RETURNED, null);
		actAs(manager);
		orderService.changeStatus(order.getId(), OrderStatus.REFUNDED, null);

		Order refunded = orderRepository.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.REFUNDED, refunded.getStatus());
		assertEquals(0, productRepository.findById(firstProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(0, productRepository.findById(secondProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(refunded.getTotalAmount(), refunded.getRefundAmount());
		assertNotNull(refunded.getRefundedAt());
		assertEquals(commissionRate, refunded.getCommissionRate());
		assertEquals(commissionAmount, refunded.getCommissionAmount());
		assertEquals(shopRevenue, refunded.getShopRevenue());
	}

	@Test
	void managerCanApproveComplaintButBuyerCannot() {
		deliverOrder();
		as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED, TRANSITION_REASON));
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REJECTED, TRANSITION_REASON));
		assertThrows(ForbiddenException.class,
				() -> as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_APPROVED,
						TRANSITION_REASON)));
		as(manager, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_APPROVED, TRANSITION_REASON));
		assertEquals(OrderStatus.RETURN_APPROVED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
	}

	@Test
	void deliveryFailureCanBeCancelledByManagerAndRestocks() {
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null));
		as(manager, () -> orderService.assignShipper(order.getId(), shipper.getId()));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.SHIPPING, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.DELIVERY_FAILED, TRANSITION_REASON));
		as(manager, () -> orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, TRANSITION_REASON));
		assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		assertStock(10, 5);
		assertEquals(0, productRepository.findById(firstProduct.getId()).orElseThrow().getSoldCount());
		assertEquals(0, productRepository.findById(secondProduct.getId()).orElseThrow().getSoldCount());
	}

	@Test
	void concurrentCancellationRestocksExactlyOnce() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<AttemptResult> buyerAttempt = executor.submit(() -> concurrentCancel(buyer, ready, start));
			Future<AttemptResult> ownerAttempt = executor.submit(() -> concurrentCancel(owner, ready, start));
			assertTrue(ready.await(15, TimeUnit.SECONDS), "Both cancellation workers should be ready");
			start.countDown();
			List<AttemptResult> results = List.of(buyerAttempt.get(30, TimeUnit.SECONDS), ownerAttempt.get(30, TimeUnit.SECONDS));
			assertEquals(1, results.stream().filter(AttemptResult::succeeded).count());
			assertEquals(1, results.stream().filter(result -> result.failure() instanceof BusinessException
					|| result.failure() instanceof ObjectOptimisticLockingFailureException).count());
			assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
			assertStock(10, 5);
			assertEquals(2, historyCount());
		} finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	private AttemptResult concurrentCancel(User actor, CountDownLatch ready, CountDownLatch start) {
		try {
			actAs(actor);
			ready.countDown();
			if (!start.await(15, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Concurrent workers did not receive start signal");
			}
			orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, CANCELLATION_REASON);
			return new AttemptResult(true, null);
		} catch (RuntimeException failure) {
			return new AttemptResult(false, failure);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void deliverOrder() {
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null));
		as(manager, () -> orderService.assignShipper(order.getId(), shipper.getId()));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.SHIPPING, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, null));
	}

	private void actAs(User user) {
		CustomUserDetails principal = new CustomUserDetails(userRepository.findByIdWithRoles(user.getId()).orElseThrow());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

	private void as(User user, Runnable action) {
		actAs(user);
		try {
			action.run();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private Role role(RoleName name) {
		return roleRepository.findByName(name).orElseGet(() -> {
			Role created = new Role();
			created.setName(name);
			created.setDescription(prefix + "-role");
			Role saved = roleRepository.saveAndFlush(created);
			createdRoleIds.add(saved.getId());
			return saved;
		});
	}

	private User saveUser(String kind, Role... roles) {
		User user = new User();
		user.setEmail(prefix + "-" + kind + "@example.test");
		user.setFullName(prefix + "-" + kind);
		user.setPassword("not-used-in-test");
		user.setStatus(UserStatus.ACTIVE);
		user.setRoles(Set.of(roles));
		return userRepository.saveAndFlush(user);
	}

	private Product saveProduct(String kind, int remainingStock) {
		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(prefix + "-product-" + kind);
		product.setPrice(kind.equals("first") ? FIRST_UNIT_PRICE : SECOND_UNIT_PRICE);
		product.setStock(remainingStock);
		product.setSoldCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.saveAndFlush(product);
	}

	private OrderItem orderItem(Order parent, Product product, BigDecimal price, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(parent);
		item.setProduct(product);
		item.setProductName(product.getName());
		item.setPrice(price);
		item.setQuantity(quantity);
		item.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
		return item;
	}

	private void assertStock(int first, int second) {
		assertEquals(first, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(second, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
	}

	private int historyCount() {
		Integer count = jdbcTemplate.queryForObject("select count(*) from order_status_histories where order_id = ?",
				Integer.class, order.getId());
		return count == null ? 0 : count;
	}

	private void assertHistory(List<HistoryRow> expected) {
		List<HistoryRow> actual = jdbcTemplate.query(
				"select previous_status, new_status, changed_by from order_status_histories where order_id = ? order by id",
				(result, rowNumber) -> new HistoryRow(
						result.getString("previous_status") == null ? null
								: OrderStatus.valueOf(result.getString("previous_status")),
						OrderStatus.valueOf(result.getString("new_status")),
						result.getObject("changed_by", Long.class)), order.getId());
		assertEquals(expected, actual);
		assertEquals(6, actual.size());
	}

	@AfterEach
	void cleanUpIntegrationData() {
		SecurityContextHolder.clearContext();
		if (order != null && order.getId() != null) {
			jdbcTemplate.update("delete from order_status_histories where order_id = ?", order.getId());
			jdbcTemplate.update("delete from order_items where order_id = ?", order.getId());
			orderRepository.deleteById(order.getId());
		}
		if (group != null && group.getId() != null) {
			orderGroupRepository.deleteById(group.getId());
		}
		if (firstProduct != null && firstProduct.getId() != null) {
			productRepository.deleteById(firstProduct.getId());
		}
		if (secondProduct != null && secondProduct.getId() != null) {
			productRepository.deleteById(secondProduct.getId());
		}
		if (shop != null && shop.getId() != null) {
			shopRepository.deleteById(shop.getId());
		}
		List<User> fixtureUsers = java.util.Arrays.asList(buyer, owner, shipper, manager).stream()
				.filter(user -> user != null).toList();
		for (User user : fixtureUsers) {
			jdbcTemplate.update("delete from user_roles where user_id = ?", user.getId());
		}
		fixtureUsers.forEach(user -> userRepository.deleteById(user.getId()));
		createdRoleIds.forEach(roleRepository::deleteById);
		createdRoleIds.clear();
	}

	private record HistoryRow(OrderStatus previous, OrderStatus current, Long actorId) {
	}

	private record AttemptResult(boolean succeeded, RuntimeException failure) {
	}
}
