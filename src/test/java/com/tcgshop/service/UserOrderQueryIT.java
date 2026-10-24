package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.UserOrderListResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
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
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Chạy bằng mvnw test -DrunIT=true. */
@SpringBootTest
@EnabledIfSystemProperty(named = "runIT", matches = "true")
class UserOrderQueryIT {
	private static final List<OrderStatus> BUYER_A_STATUSES = List.of(OrderStatus.NEW, OrderStatus.CONFIRMED,
			OrderStatus.PICKED_UP, OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED,
			OrderStatus.CANCELLED, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
			OrderStatus.RETURN_REJECTED, OrderStatus.RETURNED, OrderStatus.REFUNDED);
	private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 10, 0);

	@Autowired private UserOrderService userOrderService;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderGroupRepository groupRepository;
	@Autowired private OrderItemRepository itemRepository;
	@Autowired private OrderStatusHistoryRepository historyRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private EntityManagerFactory entityManagerFactory;

	private String prefix;
	private User buyerA;
	private User buyerB;
	private User owner;
	private Shop shop;
	private Product firstProduct;
	private Product secondProduct;
	private final List<Long> orderIds = new ArrayList<>();
	private final List<Long> groupIds = new ArrayList<>();
	private final List<Long> userIds = new ArrayList<>();
	private final List<Long> createdRoleIds = new ArrayList<>();
	private final Map<Integer, Long> buyerAOrderIds = new java.util.HashMap<>();
	private Long buyerBOrderId;

	@BeforeEach
	void setUpData() {
		prefix = "it-" + UUID.randomUUID().toString().replace("-", "");
		Role userRole = role(RoleName.ROLE_USER);
		Role vendorRole = role(RoleName.ROLE_VENDOR);
		buyerA = saveUser("buyer-a", userRole);
		buyerB = saveUser("buyer-b", userRole);
		owner = saveUser("owner", userRole, vendorRole);

		shop = new Shop();
		shop.setOwner(owner);
		shop.setName(prefix + "-shop");
		shop.setSlug(prefix + "-shop");
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		shop = shopRepository.saveAndFlush(shop);
		firstProduct = saveProduct("first");
		secondProduct = saveProduct("second");

		OrderGroup sharedGroup = saveGroup(buyerA, "shared");
		for (int index = 0; index < BUYER_A_STATUSES.size(); index++) {
			OrderGroup group = index < 2 ? sharedGroup : saveGroup(buyerA, "group-" + index);
			Order order = saveOrder(buyerA, group, BUYER_A_STATUSES.get(index), index);
			buyerAOrderIds.put(index, order.getId());
			if (index == 0) {
				saveHistory(order, null, OrderStatus.NEW, null, "it-system-created", 0);
			} else if (index == 1) {
				saveHistory(order, null, OrderStatus.NEW, null, "it-system-created", 0);
				saveHistory(order, OrderStatus.CONFIRMED, OrderStatus.CONFIRMED, owner,
						"it-assignment-private-name", 1);
			}
		}
		OrderGroup otherGroup = saveGroup(buyerB, "other-group");
		Order otherOrder = saveOrder(buyerB, otherGroup, OrderStatus.NEW, 20);
		buyerBOrderId = otherOrder.getId();
		saveHistory(otherOrder, null, OrderStatus.NEW, buyerB, "it-other-created", 0);
	}

	@Test
	void listCountsTabsFiltersPaginatesSearchesLiterallyAndRestrictsOwnership() {
		actAs(buyerA);
		UserOrderListResponse firstPage = userOrderService.listMyOrders("all", null, 0);
		UserOrderListResponse secondPage = userOrderService.listMyOrders("all", null, 1);
		assertEquals(12, tab(firstPage, "all").count());
		assertEquals(1, tab(firstPage, "new").count());
		assertEquals(1, tab(firstPage, "confirmed").count());
		assertEquals(3, tab(firstPage, "shipping").count());
		assertEquals(1, tab(firstPage, "delivered").count());
		assertEquals(1, tab(firstPage, "cancelled").count());
		assertEquals(5, tab(firstPage, "return").count());
		assertTrue(firstPage.tabs().stream().filter(value -> value.slug().equals("all")).findFirst().orElseThrow().active());
		assertEquals(10, firstPage.orders().content().size());
		assertEquals(2, secondPage.orders().content().size());
		assertEquals(buyerAOrderIds.get(11), firstPage.orders().content().get(0).orderId());
		assertEquals(buyerAOrderIds.get(0), secondPage.orders().content().get(1).orderId());
		assertEquals(5, firstPage.orders().content().get(0).itemCount());
		assertEquals(3, firstPage.orders().content().get(0).previewItems().size());
		assertEquals(2, firstPage.orders().content().get(0).moreItemsCount());

		for (int index = 0; index < BUYER_A_STATUSES.size(); index++) {
			OrderStatus status = BUYER_A_STATUSES.get(index);
			String slug = tabSlug(status);
			UserOrderListResponse filtered = userOrderService.listMyOrders(slug, null, 0);
			assertTrue(filtered.orders().content().stream().allMatch(item -> item.status() == status
					|| (slug.equals("shipping") && Set.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
							OrderStatus.DELIVERY_FAILED).contains(item.status()))
					|| (slug.equals("return") && item.status().name().startsWith("RETURN"))
					|| (slug.equals("return") && item.status() == OrderStatus.REFUNDED)));
		}
		assertEquals(List.of(buyerAOrderIds.get(2)), userOrderService.listMyOrders("all", "%", 0)
				.orders().content().stream().map(item -> item.orderId()).toList());
		assertEquals(List.of(buyerAOrderIds.get(3)), userOrderService.listMyOrders("all", "_", 0)
				.orders().content().stream().map(item -> item.orderId()).toList());

		assertThrows(ResourceNotFoundException.class, () -> userOrderService.getMyOrder(buyerBOrderId));
		actAs(owner);
		assertThrows(ResourceNotFoundException.class, () -> userOrderService.getMyOrder(buyerAOrderIds.get(0)));
	}

	@Test
	void detailKeepsSystemAndAssignmentTimelineRowsAndUsesAtMostFiveQueries() {
		actAs(buyerA);
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		sessionFactory.getStatistics().clear();
		OrderDetailResponse detail = userOrderService.getMyOrder(buyerAOrderIds.get(1));
		long statements = sessionFactory.getStatistics().getPrepareStatementCount();
		assertTrue(statements <= 5, "Expected at most five SQL statements, got " + statements);
		assertEquals(2, detail.timeline().size());
		assertEquals("Hệ thống", detail.timeline().get(0).actorLabel());
		assertEquals("Đã phân công người giao hàng", detail.timeline().get(1).statusLabel());
		assertEquals(null, detail.timeline().get(1).note());
		assertEquals(1, detail.siblingOrders().size());
		assertEquals(buyerAOrderIds.get(0), detail.siblingOrders().get(0).orderId());
	}

	@Test
	void listUsesAtMostFourSqlStatementsRegardlessOfOrderCount() {
		actAs(buyerA);
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		sessionFactory.getStatistics().clear();
		UserOrderListResponse page = userOrderService.listMyOrders("all", null, 0);
		long statements = sessionFactory.getStatistics().getPrepareStatementCount();
		assertEquals(10, page.orders().content().size());
		assertTrue(statements <= 4, "Expected at most four SQL statements, got " + statements);
	}

	private OrderTabResponseView tab(UserOrderListResponse response, String slug) {
		var tab = response.tabs().stream().filter(candidate -> candidate.slug().equals(slug)).findFirst().orElseThrow();
		return new OrderTabResponseView(tab.count());
	}

	private String tabSlug(OrderStatus status) {
		return switch (status) {
			case NEW -> "new";
			case CONFIRMED -> "confirmed";
			case PICKED_UP, SHIPPING, DELIVERY_FAILED -> "shipping";
			case DELIVERED -> "delivered";
			case CANCELLED -> "cancelled";
			case RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, RETURNED, REFUNDED -> "return";
		};
	}

	private Role role(RoleName name) {
		return roleRepository.findByName(name).orElseGet(() -> {
			Role role = new Role();
			role.setName(name);
			role.setDescription(prefix + "-role");
			Role saved = roleRepository.saveAndFlush(role);
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
		User saved = userRepository.saveAndFlush(user);
		userIds.add(saved.getId());
		return saved;
	}

	private Product saveProduct(String suffix) {
		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(prefix + "-product-" + suffix);
		product.setPrice(new BigDecimal("1000"));
		product.setStock(50);
		product.setSoldCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.saveAndFlush(product);
	}

	private OrderGroup saveGroup(User buyer, String suffix) {
		OrderGroup group = new OrderGroup();
		group.setGroupCode(prefix + "-" + suffix);
		group.setUser(buyer);
		group.setTotalAmount(new BigDecimal("10000"));
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken(UUID.randomUUID().toString());
		OrderGroup saved = groupRepository.saveAndFlush(group);
		groupIds.add(saved.getId());
		return saved;
	}

	private Order saveOrder(User buyer, OrderGroup group, OrderStatus status, int index) {
		Order order = new Order();
		String code = prefix + "-A-" + index;
		if (index == 2) code += "%literal";
		if (index == 3) code += "_literal";
		if (buyer == buyerB) code = prefix + "-B-1";
		order.setOrderCode(code);
		order.setOrderGroup(group);
		order.setUser(buyer);
		order.setShop(shop);
		order.setStatus(status);
		order.setSubtotal(new BigDecimal("5000"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setShippingFee(new BigDecimal("1000"));
		order.setTotalAmount(new BigDecimal("6000"));
		Order saved = orderRepository.saveAndFlush(order);
		orderIds.add(saved.getId());
		int lineCount = index == 11 ? 5 : index % 5 + 1;
		List<OrderItem> lines = new ArrayList<>();
		for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
			Product product = lineIndex % 2 == 0 ? firstProduct : secondProduct;
			OrderItem item = new OrderItem();
			item.setOrder(saved);
			item.setProduct(product);
			item.setProductName(product.getName());
			item.setProductImage(prefix + "-image-" + lineIndex);
			item.setCardCondition(CardCondition.NEAR_MINT);
			item.setPrice(new BigDecimal("1000"));
			item.setQuantity(1);
			item.setTotalPrice(new BigDecimal("1000"));
			lines.add(item);
		}
		itemRepository.saveAllAndFlush(lines);
		jdbcTemplate.update("update orders set created_at = ? where id = ?",
				Timestamp.valueOf(BASE_TIME.plusSeconds(index)), saved.getId());
		return saved;
	}

	private void saveHistory(Order order, OrderStatus previous, OrderStatus next, User actor, String note, int offset) {
		OrderStatusHistory history = new OrderStatusHistory();
		history.setOrder(order);
		history.setPreviousStatus(previous);
		history.setNewStatus(next);
		history.setChangedBy(actor);
		history.setNote(note);
		OrderStatusHistory saved = historyRepository.saveAndFlush(history);
		jdbcTemplate.update("update order_status_histories set created_at = ? where id = ?",
				Timestamp.valueOf(BASE_TIME.plusSeconds(offset)), saved.getId());
	}

	private void actAs(User user) {
		CustomUserDetails principal = new CustomUserDetails(userRepository.findByIdWithRoles(user.getId()).orElseThrow());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		for (Long orderId : orderIds) {
			jdbcTemplate.update("delete from order_status_histories where order_id = ?", orderId);
			jdbcTemplate.update("delete from order_items where order_id = ?", orderId);
			orderRepository.deleteById(orderId);
		}
		groupIds.forEach(groupRepository::deleteById);
		if (firstProduct != null) productRepository.deleteById(firstProduct.getId());
		if (secondProduct != null) productRepository.deleteById(secondProduct.getId());
		if (shop != null) shopRepository.deleteById(shop.getId());
		for (Long userId : userIds) jdbcTemplate.update("delete from user_roles where user_id = ?", userId);
		userIds.forEach(userRepository::deleteById);
		createdRoleIds.forEach(roleRepository::deleteById);
		orderIds.clear();
		groupIds.clear();
		userIds.clear();
		createdRoleIds.clear();
	}

	private record OrderTabResponseView(long count) { }
}
