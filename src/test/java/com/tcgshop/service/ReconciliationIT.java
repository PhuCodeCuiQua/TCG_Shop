package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcgshop.dto.response.ReconciliationFindingResponse;
import com.tcgshop.dto.response.ReconciliationReportResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.CouponUsage;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.CouponUsageRepository;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** Chạy bằng mvnw test -DrunIT=true. Mọi assertion lọc theo entityType và ID do test tạo. */
@SpringBootTest(properties = {"app.reconcile.run-on-startup=false", "app.reconcile.max-findings-per-check=100000"})
@EnabledIfSystemProperty(named = "runIT", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ReconciliationIT {
	private static final BigDecimal SUBTOTAL = new BigDecimal("4500.00");
	private static final BigDecimal SHIPPING = new BigDecimal("2000.00");
	private static final BigDecimal TOTAL = new BigDecimal("6500.00");

	@Autowired private OrderReconciliationService reconciliationService;
	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private OrderGroupRepository groupRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderItemRepository itemRepository;
	@Autowired private OrderStatusHistoryRepository historyRepository;
	@Autowired private CouponRepository couponRepository;
	@Autowired private CouponUsageRepository couponUsageRepository;
	@Autowired private JdbcTemplate jdbc;

	private String prefix;
	private User buyer;
	private User owner;
	private User shipper;
	private User manager;
	private Shop shop;
	private Product firstProduct;
	private Product secondProduct;
	private OrderGroup deliveredGroup;
	private OrderGroup cancelledGroup;
	private Order deliveredOrder;
	private Order cancelledOrder;
	private Long firstItemId;
	private Long secondItemId;
	private Coupon platformCoupon;
	private Coupon shopCoupon;
	private CouponUsage platformUsage;
	private CouponUsage shopUsage;
	private final List<Long> ownedRoleIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
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
		firstProduct = saveProduct("first");
		secondProduct = saveProduct("second");

		deliveredGroup = saveGroup("delivered-group");
		cancelledGroup = saveGroup("cancelled-group");
		deliveredOrder = saveOrder(deliveredGroup, "delivered-order");
		cancelledOrder = saveOrder(cancelledGroup, "cancelled-order");
		deliver(deliveredOrder);
		as(buyer, () -> orderService.changeStatus(cancelledOrder.getId(), OrderStatus.CANCELLED, prefix + "-cancel"));

		platformCoupon = saveCoupon("platform-coupon", null);
		shopCoupon = saveCoupon("shop-coupon", shop);
		platformUsage = saveUsage(platformCoupon, deliveredOrder);
		shopUsage = saveUsage(shopCoupon, deliveredOrder);
	}

	@ParameterizedTest(name = "{1} yields finding {0}")
	@MethodSource("corruptions")
	void findsExpectedReconciliationIssue(Corruption corruption, String description, String expectedCode,
			String entityType) {
		ReconciliationReportResponse cleanReport = reconciliationService.reconcile();
		assertFalse(cleanReport.findings().stream().anyMatch(f -> f.severity().name().equals("ERROR")
				&& ((f.entityType().equals("ORDER") && isOrder(f.entityId()))
						|| (f.entityType().equals("ORDER_ITEM") && isItem(f.entityId()))
						|| (f.entityType().equals("PRODUCT") && isProduct(f.entityId()))
						|| (f.entityType().equals("ORDER_GROUP") && isGroup(f.entityId()))
						|| (f.entityType().equals("COUPON") && isCoupon(f.entityId()))
						|| (f.entityType().equals("COUPON_USAGE") && isUsage(f.entityId())))),
			"Fixture should start without ERROR findings");

		apply(corruption);
		long entityId = entityIdFor(corruption);
		List<ReconciliationFindingResponse> findings = reconciliationService.reconcile().findings();
		assertTrue(findings.stream().anyMatch(f -> f.checkCode().equals(expectedCode)
				&& f.entityType().equals(entityType) && f.entityId().equals(entityId)),
			"Expected " + expectedCode + " for " + entityType + " id=" + entityId);
	}

	private static Stream<Arguments> corruptions() {
		return Stream.of(
				Arguments.of(Corruption.TOTAL, "order total formula", "ORD_TOTAL_FORMULA", "ORDER"),
				Arguments.of(Corruption.NEGATIVE, "invalid discount", "ORD_NEGATIVE", "ORDER"),
				Arguments.of(Corruption.NO_ITEMS, "order has no items", "ORD_NO_ITEMS", "ORDER"),
				Arguments.of(Corruption.ITEMS_SUM, "subtotal differs from items", "ORD_ITEMS_SUM", "ORDER"),
				Arguments.of(Corruption.ITEM_LINE, "item line formula", "ITEM_LINE", "ORDER_ITEM"),
				Arguments.of(Corruption.GROUP_TOTAL, "group total", "GROUP_TOTAL", "ORDER_GROUP"),
				Arguments.of(Corruption.COMMISSION_AMOUNT, "commission amount", "COMM_FORMULA", "ORDER"),
				Arguments.of(Corruption.COMMISSION_REVENUE, "shop revenue snapshot", "COMM_FORMULA", "ORDER"),
				Arguments.of(Corruption.PREMATURE_COMMISSION, "commission before finalization", "COMM_PREMATURE", "ORDER"),
				Arguments.of(Corruption.REFUND, "refund amount", "REFUND_FORMULA", "ORDER"),
				Arguments.of(Corruption.MONEY_SCALE, "fractional VND amount", "MONEY_SCALE", "ORDER"),
				Arguments.of(Corruption.HISTORY_MISSING, "missing status history", "HIST_MISSING", "ORDER"),
				Arguments.of(Corruption.HISTORY_MISMATCH, "history status differs", "HIST_MISMATCH", "ORDER"),
				Arguments.of(Corruption.SOLD_LOW, "sold count too low", "SOLD_COUNT_LOW", "PRODUCT"),
				Arguments.of(Corruption.SOLD_HIGH, "sold count too high", "SOLD_COUNT_HIGH", "PRODUCT"),
				Arguments.of(Corruption.STOCK, "negative stock", "STOCK_NEGATIVE", "PRODUCT"),
				Arguments.of(Corruption.COUPON_LOW, "coupon use count too low", "COUPON_USED_LOW", "COUPON"),
				Arguments.of(Corruption.COUPON_HIGH, "coupon use count too high", "COUPON_USED_HIGH", "COUPON"),
				Arguments.of(Corruption.PLATFORM_USAGE, "platform coupon allocation", "COUPON_USAGE_AMOUNT", "COUPON_USAGE"),
				Arguments.of(Corruption.SHOP_USAGE, "shop coupon allocation", "COUPON_USAGE_AMOUNT", "COUPON_USAGE"));
	}

	private void apply(Corruption corruption) {
		switch (corruption) {
			case TOTAL -> jdbc.update("UPDATE orders SET total_amount = total_amount + 1 WHERE id = ?", deliveredOrder.getId());
			case NEGATIVE -> jdbc.update("UPDATE orders SET discount_amount = subtotal + 1 WHERE id = ?", deliveredOrder.getId());
			case NO_ITEMS -> jdbc.update("DELETE FROM order_items WHERE order_id = ?", deliveredOrder.getId());
			case ITEMS_SUM -> jdbc.update("UPDATE orders SET subtotal = subtotal + 1 WHERE id = ?", deliveredOrder.getId());
			case ITEM_LINE -> jdbc.update("UPDATE order_items SET total_price = total_price + 1 WHERE id = ?", firstItemId);
			case GROUP_TOTAL -> jdbc.update("UPDATE order_groups SET total_amount = total_amount + 1 WHERE id = ?", deliveredGroup.getId());
			case COMMISSION_AMOUNT -> jdbc.update("UPDATE orders SET commission_amount = commission_amount + 1 WHERE id = ?", deliveredOrder.getId());
			case COMMISSION_REVENUE -> jdbc.update("UPDATE orders SET shop_revenue = NULL WHERE id = ?", deliveredOrder.getId());
			case PREMATURE_COMMISSION -> jdbc.update("UPDATE orders SET commission_rate = 5 WHERE id = ?", cancelledOrder.getId());
			case REFUND -> jdbc.update("UPDATE orders SET status = 'REFUNDED', refund_amount = 1, refunded_at = NULL WHERE id = ?", cancelledOrder.getId());
			case MONEY_SCALE -> jdbc.update("UPDATE orders SET total_amount = total_amount + 0.50 WHERE id = ?", deliveredOrder.getId());
			case HISTORY_MISSING -> jdbc.update("DELETE FROM order_status_histories WHERE order_id = ?", cancelledOrder.getId());
			case HISTORY_MISMATCH -> jdbc.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", cancelledOrder.getId());
			case SOLD_LOW -> jdbc.update("UPDATE products SET sold_count = 0 WHERE id = ?", firstProduct.getId());
			case SOLD_HIGH -> jdbc.update("UPDATE products SET sold_count = 100 WHERE id = ?", firstProduct.getId());
			case STOCK -> jdbc.update("UPDATE products SET stock = -1 WHERE id = ?", secondProduct.getId());
			case COUPON_LOW -> jdbc.update("UPDATE coupons SET used_quantity = 0 WHERE id = ?", platformCoupon.getId());
			case COUPON_HIGH -> jdbc.update("UPDATE coupons SET used_quantity = 5 WHERE id = ?", shopCoupon.getId());
			case PLATFORM_USAGE -> jdbc.update("UPDATE coupon_usages SET discount_amount = 1 WHERE id = ?", platformUsage.getId());
			case SHOP_USAGE -> jdbc.update("UPDATE coupon_usages SET discount_amount = 1 WHERE id = ?", shopUsage.getId());
		}
	}

	private void deliver(Order order) {
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, null));
		as(manager, () -> orderService.assignShipper(order.getId(), shipper.getId()));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.SHIPPING, null));
		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, null));
	}

	private OrderGroup saveGroup(String suffix) {
		OrderGroup group = new OrderGroup();
		group.setGroupCode(prefix + "-" + suffix);
		group.setUser(buyer);
		group.setTotalAmount(TOTAL);
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken(UUID.randomUUID().toString());
		return groupRepository.saveAndFlush(group);
	}

	private Order saveOrder(OrderGroup group, String suffix) {
		Order order = new Order();
		order.setOrderCode(prefix + "-" + suffix);
		order.setOrderGroup(group);
		order.setUser(buyer);
		order.setShop(shop);
		order.setStatus(OrderStatus.NEW);
		order.setSubtotal(SUBTOTAL);
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setPlatformDiscountAmount(BigDecimal.ZERO);
		order.setShippingFee(SHIPPING);
		order.setTotalAmount(TOTAL);
		order = orderRepository.saveAndFlush(order);
		OrderItem first = item(order, firstProduct, new BigDecimal("1000.00"), 2);
		OrderItem second = item(order, secondProduct, new BigDecimal("2500.00"), 1);
		List<OrderItem> savedItems = itemRepository.saveAllAndFlush(List.of(first, second));
		if (suffix.equals("delivered-order")) {
			firstItemId = savedItems.get(0).getId();
			secondItemId = savedItems.get(1).getId();
		}
		OrderStatusHistory history = new OrderStatusHistory();
		history.setOrder(order);
		history.setNewStatus(OrderStatus.NEW);
		history.setChangedBy(buyer);
		history.setNote(prefix + "-created");
		historyRepository.saveAndFlush(history);
		return order;
	}

	private OrderItem item(Order order, Product product, BigDecimal price, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setProductName(product.getName());
		item.setPrice(price);
		item.setQuantity(quantity);
		item.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
		return item;
	}

	private Coupon saveCoupon(String suffix, Shop couponShop) {
		Coupon coupon = new Coupon();
		coupon.setShop(couponShop);
		coupon.setCode(prefix + "-" + suffix);
		coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
		coupon.setDiscountValue(BigDecimal.ZERO);
		coupon.setMinOrderValue(BigDecimal.ZERO);
		coupon.setTotalQuantity(10);
		coupon.setUsedQuantity(1);
		coupon.setLimitPerUser(1);
		coupon.setStatus(ActiveStatus.ACTIVE);
		return couponRepository.saveAndFlush(coupon);
	}

	private CouponUsage saveUsage(Coupon coupon, Order order) {
		CouponUsage usage = new CouponUsage();
		usage.setCoupon(coupon);
		usage.setUser(buyer);
		usage.setOrder(order);
		usage.setDiscountAmount(BigDecimal.ZERO);
		return couponUsageRepository.saveAndFlush(usage);
	}

	private Product saveProduct(String suffix) {
		Product product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(prefix + "-product-" + suffix);
		product.setPrice(new BigDecimal("1000.00"));
		product.setStock(10);
		product.setSoldCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		return productRepository.saveAndFlush(product);
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

	private Role role(RoleName name) {
		return roleRepository.findByName(name).orElseGet(() -> {
			Role created = new Role();
			created.setName(name);
			created.setDescription(prefix + "-role");
			Role saved = roleRepository.saveAndFlush(created);
			ownedRoleIds.add(saved.getId());
			return saved;
		});
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

	private long entityIdFor(Corruption corruption) {
		return switch (corruption) {
			case TOTAL, NEGATIVE, NO_ITEMS, ITEMS_SUM, COMMISSION_AMOUNT, COMMISSION_REVENUE, MONEY_SCALE -> deliveredOrder.getId();
			case PREMATURE_COMMISSION, REFUND, HISTORY_MISSING, HISTORY_MISMATCH -> cancelledOrder.getId();
			case ITEM_LINE -> firstItemId;
			case GROUP_TOTAL -> deliveredGroup.getId();
			case SOLD_LOW, SOLD_HIGH -> firstProduct.getId();
			case STOCK -> secondProduct.getId();
			case COUPON_LOW -> platformCoupon.getId();
			case COUPON_HIGH -> shopCoupon.getId();
			case PLATFORM_USAGE -> platformUsage.getId();
			case SHOP_USAGE -> shopUsage.getId();
		};
	}

	private boolean isOrder(Long id) { return id.equals(deliveredOrder.getId()) || id.equals(cancelledOrder.getId()); }
	private boolean isItem(Long id) { return id.equals(firstItemId) || id.equals(secondItemId); }
	private boolean isProduct(Long id) { return id.equals(firstProduct.getId()) || id.equals(secondProduct.getId()); }
	private boolean isGroup(Long id) { return id.equals(deliveredGroup.getId()) || id.equals(cancelledGroup.getId()); }
	private boolean isCoupon(Long id) { return id.equals(platformCoupon.getId()) || id.equals(shopCoupon.getId()); }
	private boolean isUsage(Long id) { return id.equals(platformUsage.getId()) || id.equals(shopUsage.getId()); }

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		if (platformUsage != null) couponUsageRepository.delete(platformUsage);
		if (shopUsage != null) couponUsageRepository.delete(shopUsage);
		if (platformCoupon != null) couponRepository.delete(platformCoupon);
		if (shopCoupon != null) couponRepository.delete(shopCoupon);
		if (deliveredOrder != null) {
			jdbc.update("DELETE FROM order_status_histories WHERE order_id = ?", deliveredOrder.getId());
			jdbc.update("DELETE FROM order_items WHERE order_id = ?", deliveredOrder.getId());
			orderRepository.deleteById(deliveredOrder.getId());
		}
		if (cancelledOrder != null) {
			jdbc.update("DELETE FROM order_status_histories WHERE order_id = ?", cancelledOrder.getId());
			jdbc.update("DELETE FROM order_items WHERE order_id = ?", cancelledOrder.getId());
			orderRepository.deleteById(cancelledOrder.getId());
		}
		if (deliveredGroup != null) groupRepository.delete(deliveredGroup);
		if (cancelledGroup != null) groupRepository.delete(cancelledGroup);
		if (firstProduct != null) productRepository.delete(firstProduct);
		if (secondProduct != null) productRepository.delete(secondProduct);
		if (shop != null) shopRepository.delete(shop);
		for (User user : new User[] {buyer, owner, shipper, manager}) {
			if (user != null) {
				jdbc.update("DELETE FROM user_roles WHERE user_id = ?", user.getId());
				userRepository.delete(user);
			}
		}
		ownedRoleIds.forEach(roleRepository::deleteById);
	}

	private enum Corruption {
		TOTAL, NEGATIVE, NO_ITEMS, ITEMS_SUM, ITEM_LINE, GROUP_TOTAL, COMMISSION_AMOUNT, COMMISSION_REVENUE,
		PREMATURE_COMMISSION, REFUND, MONEY_SCALE, HISTORY_MISSING, HISTORY_MISMATCH, SOLD_LOW, SOLD_HIGH,
		STOCK, COUPON_LOW, COUPON_HIGH, PLATFORM_USAGE, SHOP_USAGE
	}

}
