package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.dto.response.CancelOrderResultResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CancelReason;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderGroupRepository;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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

/** Chạy bằng mvnw test -DrunIT=true. */
@SpringBootTest
@EnabledIfSystemProperty(named = "runIT", matches = "true")
class OrderCancellationIT {
	@Autowired private OrderCancellationService cancellationService;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderGroupRepository groupRepository;
	@Autowired private OrderItemRepository itemRepository;
	@Autowired private OrderStatusHistoryRepository historyRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private EntityManager entityManager;

	private String prefix;
	private User buyer;
	private User otherBuyer;
	private User owner;
	private Shop shop;
	private Product firstProduct;
	private Product secondProduct;
	private OrderGroup group;
	private Order order;
	private final List<Long> userIds = new ArrayList<>();
	private final List<Long> roleIds = new ArrayList<>();

	@BeforeEach
	void createFixture() {
		prefix = "it-" + UUID.randomUUID().toString().replace("-", "");
		Role userRole = role(RoleName.ROLE_USER);
		Role vendorRole = role(RoleName.ROLE_VENDOR);
		buyer = saveUser("buyer", userRole);
		otherBuyer = saveUser("other-buyer", userRole);
		owner = saveUser("owner", userRole, vendorRole);

		shop = new Shop();
		shop.setOwner(owner);
		shop.setName(prefix + "-shop");
		shop.setSlug(prefix + "-shop");
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
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
		group = groupRepository.saveAndFlush(group);

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
		OrderItem firstLine = item(order, firstProduct, new BigDecimal("1000.00"), 2);
		OrderItem secondLine = item(order, secondProduct, new BigDecimal("2500.00"), 1);
		itemRepository.saveAllAndFlush(List.of(firstLine, secondLine));
		OrderStatusHistory initial = new OrderStatusHistory();
		initial.setOrder(order);
		initial.setPreviousStatus(null);
		initial.setNewStatus(OrderStatus.NEW);
		initial.setChangedBy(buyer);
		initial.setNote(prefix + "-created");
		historyRepository.saveAndFlush(initial);
	}

	@Test
	void buyerCancelsNewOrderRestocksAndRepeatedCancellationIsIdempotent() {
		Long firstVersion = productRepository.findById(firstProduct.getId()).orElseThrow().getVersion();
		Long secondVersion = productRepository.findById(secondProduct.getId()).orElseThrow().getVersion();
		actAs(buyer);
		CancelOrderResultResponse result = cancellationService.cancelMyOrder(order.getId(),
				request(CancelReason.OTHER, "  I   changed  plans  "));
		entityManager.clear();

		Order cancelled = orderRepository.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
		assertEquals("Lý do khác: I changed plans", cancelled.getCancelReason());
		assertEquals(new CancelOrderResultResponse(order.getId(), order.getOrderCode(), false), result);
		assertEquals(10, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(5, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
		assertEquals(firstVersion + 1, productRepository.findById(firstProduct.getId()).orElseThrow().getVersion());
		assertEquals(secondVersion + 1, productRepository.findById(secondProduct.getId()).orElseThrow().getVersion());
		assertEquals(buyer.getId(), jdbcTemplate.queryForObject(
				"select changed_by from order_status_histories where order_id = ? order by id desc limit 1",
				Long.class, order.getId()));
		int historyCount = historyCount();

		CancelOrderResultResponse repeated = cancellationService.cancelMyOrder(order.getId(),
				request(CancelReason.CHANGED_MIND, null));
		entityManager.clear();
		assertTrue(repeated.alreadyCancelled());
		assertEquals(10, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(5, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
		assertEquals(historyCount, historyCount());
	}

	@Test
	void confirmedOrderCanBeCancelled() {
		setStatus(OrderStatus.CONFIRMED);
		actAs(buyer);
		cancellationService.cancelMyOrder(order.getId(), request(CancelReason.CHANGED_MIND, null));
		entityManager.clear();
		assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(CancelReason.CHANGED_MIND.getLabel(), orderRepository.findById(order.getId()).orElseThrow().getCancelReason());
	}

	@Test
	void invalidLaterStatusDoesNotChangeOrderStockOrHistory() {
		for (OrderStatus status : List.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING)) {
			setStatus(status);
			int historyBefore = historyCount();
			actAs(buyer);
			assertThrows(BusinessException.class, () -> cancellationService.cancelMyOrder(order.getId(),
					request(CancelReason.CHANGED_MIND, null)));
			entityManager.clear();
			assertEquals(status, orderRepository.findById(order.getId()).orElseThrow().getStatus());
			assertEquals(8, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
			assertEquals(4, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
			assertEquals(historyBefore, historyCount());
		}
	}

	@Test
	void otherBuyerAndShopOwnerReceiveNotFound() {
		actAs(otherBuyer);
		assertThrows(ResourceNotFoundException.class, () -> cancellationService.cancelMyOrder(order.getId(),
				request(CancelReason.CHANGED_MIND, null)));
		actAs(owner);
		assertThrows(ResourceNotFoundException.class, () -> cancellationService.cancelMyOrder(order.getId(),
				request(CancelReason.CHANGED_MIND, null)));
		entityManager.clear();
		assertEquals(OrderStatus.NEW, orderRepository.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(8, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
		assertEquals(4, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
		assertEquals(1, historyCount());
	}

	@Test
	void concurrentBuyerCancellationRestocksExactlyOnce() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Attempt> first = executor.submit(() -> cancelConcurrently(ready, start));
			Future<Attempt> second = executor.submit(() -> cancelConcurrently(ready, start));
			assertTrue(ready.await(15, TimeUnit.SECONDS));
			start.countDown();
			List<Attempt> attempts = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
			assertEquals(2, attempts.stream().filter(Attempt::acceptable).count());
			assertTrue(attempts.stream().anyMatch(attempt -> attempt.alreadyCancelled() != null));
			entityManager.clear();
			assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
			assertEquals(10, productRepository.findById(firstProduct.getId()).orElseThrow().getStock());
			assertEquals(5, productRepository.findById(secondProduct.getId()).orElseThrow().getStock());
			assertEquals(2, historyCount());
		} finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	private Attempt cancelConcurrently(CountDownLatch ready, CountDownLatch start) {
		actAs(buyer);
		ready.countDown();
		try {
			if (!start.await(15, TimeUnit.SECONDS)) return new Attempt(false, null);
			CancelOrderResultResponse response = cancellationService.cancelMyOrder(order.getId(),
					request(CancelReason.CHANGED_MIND, null));
			return new Attempt(true, response.alreadyCancelled());
		} catch (BusinessException | ObjectOptimisticLockingFailureException expectedRace) {
			return new Attempt(true, null);
		} catch (RuntimeException failure) {
			return new Attempt(false, null);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return new Attempt(false, null);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void setStatus(OrderStatus status) {
		jdbcTemplate.update("update orders set status = ? where id = ?", status.name(), order.getId());
		entityManager.clear();
		order = orderRepository.findById(order.getId()).orElseThrow();
	}

	private int historyCount() {
		Integer count = jdbcTemplate.queryForObject(
				"select count(*) from order_status_histories where order_id = ?", Integer.class, order.getId());
		return count == null ? 0 : count;
	}

	private void actAs(User user) {
		CustomUserDetails principal = new CustomUserDetails(userRepository.findByIdWithRoles(user.getId()).orElseThrow());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

	private Role role(RoleName name) {
		return roleRepository.findByName(name).orElseGet(() -> {
			Role created = new Role();
			created.setName(name);
			created.setDescription(prefix + "-role");
			Role saved = roleRepository.saveAndFlush(created);
			roleIds.add(saved.getId());
			return saved;
		});
	}

	private User saveUser(String suffix, Role... roles) {
		User user = new User();
		user.setEmail(prefix + "-" + suffix + "@example.test");
		user.setFullName(prefix + "-" + suffix);
		user.setPassword("not-used-in-test");
		user.setStatus(UserStatus.ACTIVE);
		user.setRoles(Set.of(roles));
		User saved = userRepository.saveAndFlush(user);
		userIds.add(saved.getId());
		return saved;
	}

	private Product saveProduct(String suffix, int stock) {
		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(prefix + "-product-" + suffix);
		product.setPrice(new BigDecimal("1000.00"));
		product.setStock(stock);
		product.setSoldCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.saveAndFlush(product);
	}

	private OrderItem item(Order parent, Product product, BigDecimal price, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(parent);
		item.setProduct(product);
		item.setProductName(product.getName());
		item.setPrice(price);
		item.setQuantity(quantity);
		item.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
		return item;
	}

	private CancelOrderRequest request(CancelReason reason, String detail) {
		CancelOrderRequest request = new CancelOrderRequest();
		request.setReason(reason);
		request.setDetail(detail);
		return request;
	}

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		if (order != null && order.getId() != null) {
			jdbcTemplate.update("delete from order_status_histories where order_id = ?", order.getId());
			jdbcTemplate.update("delete from order_items where order_id = ?", order.getId());
			orderRepository.deleteById(order.getId());
		}
		if (group != null && group.getId() != null) groupRepository.deleteById(group.getId());
		if (firstProduct != null && firstProduct.getId() != null) productRepository.deleteById(firstProduct.getId());
		if (secondProduct != null && secondProduct.getId() != null) productRepository.deleteById(secondProduct.getId());
		if (shop != null && shop.getId() != null) shopRepository.deleteById(shop.getId());
		for (Long userId : userIds) jdbcTemplate.update("delete from user_roles where user_id = ?", userId);
		userIds.forEach(userRepository::deleteById);
		roleIds.forEach(roleRepository::deleteById);
		userIds.clear();
		roleIds.clear();
	}

	private record Attempt(boolean acceptable, Boolean alreadyCancelled) { }
}
