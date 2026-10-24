package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ReturnStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.repository.OrderGroupRepository;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ReturnRequestRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Run with mvnw test -DrunIT=true. */
@SpringBootTest
@EnabledIfSystemProperty(named = "runIT", matches = "true")
class ReturnRequestSyncIT {
	@Autowired private OrderService orderService;
	@Autowired private OrderRepository orderRepository;
	@Autowired private OrderGroupRepository orderGroupRepository;
	@Autowired private OrderItemRepository orderItemRepository;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private ProductRepository productRepository;
	@Autowired private ReturnRequestRepository returnRequestRepository;
	@Autowired private ShopRepository shopRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;

	private String prefix;
	private User buyer;
	private User owner;
	private User shipper;
	private User manager;
	private Shop shop;
	private Product product;
	private OrderGroup group;
	private Order order;
	private ReturnRequest request;
	private final List<Long> createdRoleIds = new ArrayList<>();

	@BeforeEach
	void createFixture() {
		prefix = "it-" + UUID.randomUUID().toString().replace("-", "");
		Role userRole = role(RoleName.ROLE_USER);
		buyer = saveUser("buyer", userRole);
		owner = saveUser("owner", userRole, role(RoleName.ROLE_VENDOR));
		shipper = saveUser("shipper", userRole, role(RoleName.ROLE_SHIPPER));
		manager = saveUser("manager", role(RoleName.ROLE_MANAGER));

		shop = new Shop();
		shop.setOwner(owner);
		shop.setName(prefix + "-shop");
		shop.setSlug(prefix + "-shop");
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		shop.setCommissionRate(new BigDecimal("5.00"));
		shop = shopRepository.saveAndFlush(shop);

		product = new Product();
		product.setShop(shop);
		product.setType(ProductType.SUPPLY);
		product.setName(prefix + "-product");
		product.setPrice(new BigDecimal("1000.00"));
		product.setStock(9);
		product.setSoldCount(1);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product = productRepository.saveAndFlush(product);

		group = new OrderGroup();
		group.setGroupCode(prefix + "-group");
		group.setUser(buyer);
		group.setTotalAmount(new BigDecimal("1000.00"));
		group.setPaymentMethod(PaymentMethod.COD);
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken(UUID.randomUUID().toString());
		group = orderGroupRepository.saveAndFlush(group);

		order = new Order();
		order.setOrderCode(prefix + "-order");
		order.setOrderGroup(group);
		order.setUser(buyer);
		order.setShop(shop);
		order.setShipper(shipper);
		order.setStatus(OrderStatus.DELIVERED);
		order.setSubtotal(new BigDecimal("1000.00"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setPlatformDiscountAmount(BigDecimal.ZERO);
		order.setShippingFee(BigDecimal.ZERO);
		order.setTotalAmount(new BigDecimal("1000.00"));
		order.setDeliveredAt(java.time.LocalDateTime.now());
		order.setCommissionRate(new BigDecimal("5.00"));
		order.setCommissionAmount(new BigDecimal("50.00"));
		order.setShopRevenue(new BigDecimal("950.00"));
		order = orderRepository.saveAndFlush(order);
		OrderItem line = new OrderItem();
		line.setOrder(order);
		line.setProduct(product);
		line.setProductName(product.getName());
		line.setPrice(product.getPrice());
		line.setQuantity(1);
		line.setTotalPrice(product.getPrice());
		orderItemRepository.saveAndFlush(line);

		request = new ReturnRequest();
		request.setOrder(order);
		request.setUser(buyer);
		request.setReason("it-return-reason");
		request.setStatus(ReturnStatus.PENDING);
		request = returnRequestRepository.saveAndFlush(request);
	}

	@Test
	void approvalReturnAndRefundKeepRequestInSync() {
		as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED, "it-request"));
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_APPROVED, "it-vendor-approved"));
		ReturnRequest approved = returnRequestRepository.findById(request.getId()).orElseThrow();
		assertEquals(ReturnStatus.APPROVED, approved.getStatus());
		assertEquals("it-vendor-approved", approved.getVendorNote());
		assertNotNull(approved.getDecidedAt());

		as(shipper, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURNED, null));
		assertEquals(ReturnStatus.APPROVED, returnRequestRepository.findById(request.getId()).orElseThrow().getStatus());
		as(manager, () -> orderService.changeStatus(order.getId(), OrderStatus.REFUNDED, "it-refund-reference"));
		ReturnRequest completed = returnRequestRepository.findById(request.getId()).orElseThrow();
		assertEquals(ReturnStatus.COMPLETED, completed.getStatus());
		assertEquals("Hoàn tiền: it-refund-reference", completed.getAdminNote());
	}

	@Test
	void managerApprovalAfterVendorRejectionWritesAdminNote() {
		as(buyer, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED, "it-request"));
		as(owner, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_REJECTED, "it-vendor-rejected"));
		ReturnRequest rejected = returnRequestRepository.findById(request.getId()).orElseThrow();
		assertEquals(ReturnStatus.REJECTED, rejected.getStatus());
		assertEquals("it-vendor-rejected", rejected.getVendorNote());
		rejected.setStatus(ReturnStatus.ESCALATED);
		returnRequestRepository.saveAndFlush(rejected);

		as(manager, () -> orderService.changeStatus(order.getId(), OrderStatus.RETURN_APPROVED, "it-manager-approved"));
		ReturnRequest approved = returnRequestRepository.findById(request.getId()).orElseThrow();
		assertEquals(ReturnStatus.APPROVED, approved.getStatus());
		assertEquals("it-manager-approved", approved.getAdminNote());
		assertNotNull(approved.getDecidedAt());
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

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		if (request != null && request.getId() != null) returnRequestRepository.deleteById(request.getId());
		if (order != null && order.getId() != null) {
			jdbcTemplate.update("delete from order_status_histories where order_id = ?", order.getId());
			jdbcTemplate.update("delete from order_items where order_id = ?", order.getId());
			orderRepository.deleteById(order.getId());
		}
		if (group != null && group.getId() != null) orderGroupRepository.deleteById(group.getId());
		if (product != null && product.getId() != null) productRepository.deleteById(product.getId());
		if (shop != null && shop.getId() != null) shopRepository.deleteById(shop.getId());
		for (User user : java.util.Arrays.asList(buyer, owner, shipper, manager).stream()
				.filter(u -> u != null).toList()) {
			jdbcTemplate.update("delete from user_roles where user_id = ?", user.getId());
		}
		// Xóa liên kết role trước user theo thứ tự khóa ngoại.
		for (User user : java.util.Arrays.asList(buyer, owner, shipper, manager).stream()
				.filter(u -> u != null).toList()) {
			userRepository.deleteById(user.getId());
		}
		createdRoleIds.forEach(roleRepository::deleteById);
		createdRoleIds.clear();
	}
}
