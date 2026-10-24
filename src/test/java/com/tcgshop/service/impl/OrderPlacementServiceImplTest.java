package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.request.ShopCheckoutOption;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.dto.response.CheckoutCouponResponse;
import com.tcgshop.dto.response.CheckoutLineResponse;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.CheckoutShopResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.event.OrderCreatedEvent;
import com.tcgshop.event.OrderGroupPlacedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderGroupRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ShippingCarrierRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CheckoutService;
import com.tcgshop.service.CouponService;
import com.tcgshop.util.OrderCodeGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceImplTest {
	private static final Long BUYER_ID = 1L;
	private static final BigDecimal GRAND_TOTAL = new BigDecimal("1080");
	private static final String TOKEN = "123e4567-e89b-42d3-a456-426614174000";

	@Mock private CurrentUser currentUser;
	@Mock private UserRepository userRepository;
	@Mock private OrderGroupRepository orderGroupRepository;
	@Mock private OrderRepository orderRepository;
	@Mock private OrderStatusHistoryRepository historyRepository;
	@Mock private ProductRepository productRepository;
	@Mock private ShippingCarrierRepository carrierRepository;
	@Mock private CheckoutService checkoutService;
	@Mock private CartService cartService;
	@Mock private CouponService couponService;
	@Mock private OrderCodeGenerator codeGenerator;
	@Mock private ApplicationEventPublisher eventPublisher;

	private final User buyer = buyer();
	private OrderPlacementServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OrderPlacementServiceImpl(currentUser, userRepository, orderGroupRepository,
				orderRepository, historyRepository, productRepository, carrierRepository, checkoutService,
				cartService, couponService, codeGenerator, eventPublisher);
		when(currentUser.getId()).thenReturn(BUYER_ID);
		lenient().when(userRepository.findByIdForUpdate(BUYER_ID)).thenReturn(Optional.of(buyer));
	}

	@Test
	void placesTwoShopOrdersWithSnapshotsHistoriesAndEvents() {
		CheckoutPreviewResponse preview = preview(
				new CheckoutCouponResponse("SHOP", true, 300L, new BigDecimal("20"), "Đã áp dụng"),
				new CheckoutCouponResponse("PLATFORM", true, 400L, new BigDecimal("30"), "Đã áp dụng"), true);
		prepareSuccessfulPlacement(preview);

		var response = service.placeOrder(requestWithBothCoupons(GRAND_TOTAL));

		assertEquals(500L, response.orderGroupId());
		assertEquals("GRP260101ABC234", response.groupCode());
		assertEquals(PaymentMethod.COD, response.paymentMethod());
		assertEquals(0, GRAND_TOTAL.compareTo(response.grandTotal()));
		assertFalse(response.alreadyPlaced());
		assertEquals("/checkout/success/GRP260101ABC234", response.redirectUrl());
		assertEquals(2, response.orders().size());
		ArgumentCaptor<OrderGroup> groupCaptor = ArgumentCaptor.forClass(OrderGroup.class);
		verify(orderGroupRepository).saveAndFlush(groupCaptor.capture());
		assertEquals(GRAND_TOTAL, groupCaptor.getValue().getTotalAmount());
		assertEquals(PaymentStatus.PENDING, groupCaptor.getValue().getPaymentStatus());
		assertEquals(TOKEN, groupCaptor.getValue().getIdempotencyToken());

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository, times(2)).saveAndFlush(orderCaptor.capture());
		List<Order> orders = orderCaptor.getAllValues();	
		assertEquals(OrderStatus.NEW, orders.get(0).getStatus());
		assertEquals(new BigDecimal("300"), orders.get(0).getSubtotal());
		assertEquals(new BigDecimal("30"), orders.get(0).getDiscountAmount());
		assertEquals(new BigDecimal("50"), orders.get(0).getShippingFee());
		assertEquals(new BigDecimal("320"), orders.get(0).getTotalAmount());
		assertEquals(new BigDecimal("700"), orders.get(1).getSubtotal());
		assertEquals(new BigDecimal("20"), orders.get(1).getDiscountAmount());
		assertEquals(new BigDecimal("80"), orders.get(1).getShippingFee());
		assertEquals(new BigDecimal("760"), orders.get(1).getTotalAmount());
		for (Order order : orders) {
			assertEquals("Nguyen Van A", order.getReceiverName());
			assertEquals("0900000000", order.getReceiverPhone());
			assertEquals("1 Main, Ward, District, Province", order.getShippingAddress());
			assertEquals("Xin giao giờ hành chính", order.getNote());
			assertFalse(order.isCodCollected());
			assertNull(order.getCommissionRate());
			assertNull(order.getCommissionAmount());
			assertNull(order.getShopRevenue());
			assertNull(order.getDeliveredAt());
			assertEquals(2, order.getItems().size());
			assertEquals(OrderStatus.NEW, order.getItems().get(0).getOrder().getStatus());
		}
		assertEquals(new BigDecimal("100"), orders.get(0).getItems().get(0).getPrice());
		assertEquals(new BigDecimal("100"), orders.get(0).getItems().get(0).getTotalPrice());
		assertEquals(CardCondition.NEAR_MINT, orders.get(0).getItems().get(0).getCardCondition());
		assertEquals("Product 11", orders.get(0).getItems().get(0).getProductName());
		assertEquals("https://example.test/11.png", orders.get(0).getItems().get(0).getProductImage());
		ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
		verify(historyRepository, times(2)).save(historyCaptor.capture());
		assertTrue(historyCaptor.getAllValues().stream().allMatch(history ->
				history.getPreviousStatus() == null && history.getNewStatus() == OrderStatus.NEW
						&& "Đặt hàng".equals(history.getNote()) && history.getChangedBy() == buyer));
		verify(couponService).recordUsage(300L, BUYER_ID, 600L, new BigDecimal("20"));
		verify(couponService).recordUsage(400L, BUYER_ID, 600L, new BigDecimal("30"));
		verify(cartService).removePurchasedItems(Set.of(11L, 12L, 21L, 22L));
		ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());
		List<Object> events = eventCaptor.getAllValues();
		assertEquals(2, events.stream().filter(OrderCreatedEvent.class::isInstance).count());
		OrderCreatedEvent created = events.stream().filter(OrderCreatedEvent.class::isInstance)
				.map(OrderCreatedEvent.class::cast).findFirst().orElseThrow();
		assertEquals(BUYER_ID, created.buyerId());
		assertEquals(90L, created.shopOwnerId());
		OrderGroupPlacedEvent groupEvent = events.stream().filter(OrderGroupPlacedEvent.class::isInstance)
				.map(OrderGroupPlacedEvent.class::cast).findFirst().orElseThrow();
		assertEquals("buyer@example.test", groupEvent.buyerEmail());
		assertEquals("Nguyen Van A", groupEvent.buyerName());
		assertEquals(List.of("TCG260101ABC234", "TCG260101DEF567"), groupEvent.orderCodes());
		assertEquals(GRAND_TOTAL, groupEvent.grandTotal());
	}

	@Test
	void locksUserBeforeCheckingTokenPreviewAndStock() {
		prepareSuccessfulPlacement(preview(null, null, true));

		service.placeOrder(request(null, null, GRAND_TOTAL));

		InOrder ordered = inOrder(currentUser, userRepository, orderGroupRepository, checkoutService, productRepository);
		ordered.verify(currentUser).getId();
		ordered.verify(userRepository).findByIdForUpdate(BUYER_ID);
		ordered.verify(orderGroupRepository).findByIdempotencyToken(TOKEN);
		ordered.verify(checkoutService).preview(any());
		ordered.verify(productRepository).findAllWithShopOwnerByIdIn(anyCollection());
		ordered.verify(productRepository).decreaseStock(11L, 1, ProductStatus.ACTIVE);
		ordered.verify(productRepository).decreaseStock(12L, 1, ProductStatus.ACTIVE);
		ordered.verify(productRepository).decreaseStock(21L, 1, ProductStatus.ACTIVE);
		ordered.verify(productRepository).decreaseStock(22L, 1, ProductStatus.ACTIVE);
	}

	@Test
	void idempotentRequestFromSameBuyerReturnsSavedGroupWithoutRepeatingWork() {
		OrderGroup old = new OrderGroup();
		old.setId(77L);
		old.setGroupCode("GRPOLD");
		old.setUser(buyer);
		old.setTotalAmount(new BigDecimal("42"));
		old.setPaymentMethod(PaymentMethod.COD);
		when(orderGroupRepository.findByIdempotencyToken(TOKEN)).thenReturn(Optional.of(old));
		when(orderRepository.findByOrderGroupIdOrderByIdAsc(77L)).thenReturn(List.of());

		PlaceOrderRequest upperCaseToken = new PlaceOrderRequest(null,
				List.of(new ShopCheckoutOption(10L, 100L, null), new ShopCheckoutOption(20L, 200L, null)),
				null, PaymentMethod.COD, TOKEN.toUpperCase(java.util.Locale.ROOT), GRAND_TOTAL, null);
		var response = service.placeOrder(upperCaseToken);

		assertTrue(response.alreadyPlaced());
		assertEquals(77L, response.orderGroupId());
		verify(checkoutService, never()).preview(any());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());
		verify(orderGroupRepository, never()).saveAndFlush(any());
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void idempotencyTokenOwnedByAnotherBuyerIsRejected() {
		User other = buyer();
		other.setId(99L);
		OrderGroup old = new OrderGroup();
		old.setUser(other);
		when(orderGroupRepository.findByIdempotencyToken(TOKEN)).thenReturn(Optional.of(old));

		BadRequestException error = assertThrows(BadRequestException.class,
				() -> service.placeOrder(request(null, null, GRAND_TOTAL)));

		assertEquals("Phiên thanh toán không hợp lệ, vui lòng tải lại trang", error.getMessage());
		verify(checkoutService, never()).preview(any());
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsMomoWithoutSaving() {
		BusinessException error = assertThrows(BusinessException.class,
				() -> service.placeOrder(request(PaymentMethod.MOMO, null, GRAND_TOTAL)));

		assertEquals("Thanh toán MoMo sẽ được hỗ trợ sau. Vui lòng chọn thanh toán khi nhận hàng", error.getMessage());
		verify(checkoutService, never()).preview(any());
		verify(orderGroupRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsPreviewIssuesBeforeReservingStock() {
		CheckoutPreviewResponse unavailable = preview(null, null, false);
		when(checkoutService.preview(any())).thenReturn(unavailable);

		BusinessException error = assertThrows(BusinessException.class,
				() -> service.placeOrder(request(null, null, GRAND_TOTAL)));

		assertEquals("Vui lòng chọn sản phẩm để thanh toán", error.getMessage());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());
		verify(orderGroupRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsInvalidShopAndPlatformCouponsWithPreviewMessage() {
		CheckoutCouponResponse invalid = new CheckoutCouponResponse("BAD", false, 9L, BigDecimal.ZERO,
				"Mã đã hết hạn");
		when(checkoutService.preview(any())).thenReturn(preview(invalid, null, true));

		BusinessException shopError = assertThrows(BusinessException.class,
				() -> service.placeOrder(request(null, "BAD", GRAND_TOTAL)));
		assertEquals("Mã đã hết hạn", shopError.getMessage());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());

		when(checkoutService.preview(any())).thenReturn(preview(null, invalid, true));
		BusinessException platformError = assertThrows(BusinessException.class,
				() -> service.placeOrder(requestWithPlatformCoupon("BAD", GRAND_TOTAL)));
		assertEquals("Mã đã hết hạn", platformError.getMessage());
		verify(orderGroupRepository, never()).saveAndFlush(any());
	}

	@Test
	void requiresExplicitCarrierForEveryShop() {
		when(checkoutService.preview(any())).thenReturn(preview(null, null, true));
		PlaceOrderRequest withoutCarrier = new PlaceOrderRequest(null,
				List.of(new ShopCheckoutOption(10L, null, null), new ShopCheckoutOption(20L, 200L, null)),
				null, PaymentMethod.COD, TOKEN, GRAND_TOTAL, null);

		BadRequestException error = assertThrows(BadRequestException.class, () -> service.placeOrder(withoutCarrier));

		assertEquals("Vui lòng chọn đơn vị vận chuyển cho từng cửa hàng", error.getMessage());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void totalMismatchIsConflictAndDoesNotReserveStock() {
		when(checkoutService.preview(any())).thenReturn(preview(null, null, true));

		ConflictException error = assertThrows(ConflictException.class,
				() -> service.placeOrder(request(null, null, new BigDecimal("1079"))));

		assertEquals("Giá hoặc ưu đãi đã thay đổi, vui lòng kiểm tra lại đơn hàng trước khi đặt", error.getMessage());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void rejectsBuyersOwnShopBeforeStockUpdate() {
		CheckoutPreviewResponse preview = preview(null, null, true);
		when(checkoutService.preview(any())).thenReturn(preview);
		List<Product> products = products();
		products.get(0).getShop().getOwner().setId(BUYER_ID);
		when(productRepository.findAllWithShopOwnerByIdIn(anyCollection())).thenReturn(products);

		BadRequestException error = assertThrows(BadRequestException.class,
				() -> service.placeOrder(request(null, null, GRAND_TOTAL)));

		assertEquals("Bạn không thể mua sản phẩm của cửa hàng của chính mình", error.getMessage());
		verify(productRepository, never()).decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void zeroStockUpdateExplainsInactiveAndInsufficientStock() {
		CheckoutPreviewResponse preview = oneProductPreview();
		when(checkoutService.preview(any())).thenReturn(preview);
		Product inactive = product(11L, 10, 10L, 90L);
		inactive.setStatus(ProductStatus.INACTIVE);
		when(productRepository.findAllWithShopOwnerByIdIn(anyCollection())).thenReturn(List.of(inactive));
		when(productRepository.decreaseStock(11L, 2, ProductStatus.ACTIVE)).thenReturn(0);
		when(productRepository.findById(11L)).thenReturn(Optional.of(inactive));

		BusinessException inactiveError = assertThrows(BusinessException.class,
				() -> service.placeOrder(oneProductRequest(new BigDecimal("200"))));
		assertEquals("Sản phẩm 'Product 11' không còn bán", inactiveError.getMessage());

		Product lowStock = product(11L, 1, 10L, 90L);
		when(productRepository.findAllWithShopOwnerByIdIn(anyCollection())).thenReturn(List.of(lowStock));
		when(productRepository.findById(11L)).thenReturn(Optional.of(lowStock));
		BusinessException lowError = assertThrows(BusinessException.class,
				() -> service.placeOrder(oneProductRequest(new BigDecimal("200"))));
		assertEquals("Sản phẩm 'Product 11' chỉ còn 1 sản phẩm", lowError.getMessage());
	}

	@Test
	void reportsSoldOutProductWhenCurrentStockIsZero() {
		when(checkoutService.preview(any())).thenReturn(oneProductPreview());
		Product soldOut = product(11L, 0, 10L, 90L);
		when(productRepository.findAllWithShopOwnerByIdIn(anyCollection())).thenReturn(List.of(soldOut));
		when(productRepository.decreaseStock(11L, 2, ProductStatus.ACTIVE)).thenReturn(0);
		when(productRepository.findById(11L)).thenReturn(Optional.of(soldOut));

		BusinessException error = assertThrows(BusinessException.class,
				() -> service.placeOrder(oneProductRequest(new BigDecimal("200"))));

		assertEquals("Sản phẩm 'Product 11' đã hết hàng", error.getMessage());
	}

	@Test
	void getSummaryUsesOwnerScopedLookup() {
		when(orderGroupRepository.findByGroupCodeAndUserId("GRP1", BUYER_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getPlacedSummary("GRP1"));
		verify(orderRepository, never()).findByOrderGroupIdOrderByIdAsc(anyLong());
	}

	private void prepareSuccessfulPlacement(CheckoutPreviewResponse preview) {
		when(orderGroupRepository.findByIdempotencyToken(TOKEN)).thenReturn(Optional.empty());
		when(checkoutService.preview(any())).thenReturn(preview);
		when(productRepository.findAllWithShopOwnerByIdIn(anyCollection())).thenReturn(products());
		when(productRepository.decreaseStock(anyLong(), org.mockito.ArgumentMatchers.anyInt(), eq(ProductStatus.ACTIVE)))
				.thenReturn(1);
		when(codeGenerator.generateUnique(eq("GRP"), org.mockito.ArgumentMatchers.<Predicate<String>>any()))
				.thenReturn("GRP260101ABC234");
		when(codeGenerator.generateUnique(eq("TCG"), org.mockito.ArgumentMatchers.<Predicate<String>>any()))
				.thenReturn("TCG260101ABC234", "TCG260101DEF567");
		when(orderGroupRepository.saveAndFlush(any(OrderGroup.class))).thenAnswer(invocation -> {
			OrderGroup group = invocation.getArgument(0);
			group.setId(500L);
			return group;
		});
		when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(new org.mockito.stubbing.Answer<Order>() {
			private long id = 600L;
			@Override
			public Order answer(org.mockito.invocation.InvocationOnMock invocation) {
				Order order = invocation.getArgument(0);
				order.setId(id++);
				return order;
			}
		});
		when(carrierRepository.getReferenceById(anyLong())).thenAnswer(invocation -> {
			ShippingCarrier carrier = new ShippingCarrier();
			carrier.setId(invocation.getArgument(0));
			return carrier;
		});
	}

	private PlaceOrderRequest request(PaymentMethod paymentMethod, String shopCoupon, BigDecimal expected) {
		return new PlaceOrderRequest(null,
				List.of(new ShopCheckoutOption(10L, 100L, shopCoupon), new ShopCheckoutOption(20L, 200L, null)),
				null, paymentMethod == null ? PaymentMethod.COD : paymentMethod, TOKEN, expected,
				"  Xin giao giờ hành chính  ");
	}

	private PlaceOrderRequest requestWithPlatformCoupon(String code, BigDecimal expected) {
		return new PlaceOrderRequest(null,
				List.of(new ShopCheckoutOption(10L, 100L, null), new ShopCheckoutOption(20L, 200L, null)),
				code, PaymentMethod.COD, TOKEN, expected, null);
	}

	private PlaceOrderRequest requestWithBothCoupons(BigDecimal expected) {
		return new PlaceOrderRequest(null,
				List.of(new ShopCheckoutOption(10L, 100L, "SHOP"), new ShopCheckoutOption(20L, 200L, null)),
				"PLATFORM", PaymentMethod.COD, TOKEN, expected, "  Xin giao giờ hành chính  ");
	}

	private PlaceOrderRequest oneProductRequest(BigDecimal expected) {
		return new PlaceOrderRequest(null, List.of(new ShopCheckoutOption(10L, 100L, null)), null,
				PaymentMethod.COD, TOKEN, expected, null);
	}

	private CheckoutPreviewResponse preview(CheckoutCouponResponse shopCoupon,
			CheckoutCouponResponse platformCoupon, boolean canPlace) {
		CheckoutShopResponse first = new CheckoutShopResponse(10L, "Shop 10",
				List.of(line(11L, "Product 11", 100, 1), line(12L, "Product 12", 200, 1)),
				new BigDecimal("300"), shopCoupon, new BigDecimal("10"), new BigDecimal("30"),
				100L, "Carrier 100", new BigDecimal("50"), new BigDecimal("320"), List.of());
		CheckoutShopResponse second = new CheckoutShopResponse(20L, "Shop 20",
				List.of(line(21L, "Product 21", 300, 1), line(22L, "Product 22", 400, 1)),
				new BigDecimal("700"), null, new BigDecimal("20"), new BigDecimal("20"),
				200L, "Carrier 200", new BigDecimal("80"), new BigDecimal("760"), List.of());
		return new CheckoutPreviewResponse(address(), PaymentMethod.COD, List.of(), List.of(first, second),
				platformCoupon, new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("130"),
				GRAND_TOTAL, canPlace ? List.of() : List.of("Vui lòng chọn sản phẩm để thanh toán"), canPlace);
	}

	private CheckoutPreviewResponse oneProductPreview() {
		CheckoutShopResponse shop = new CheckoutShopResponse(10L, "Shop 10",
				List.of(line(11L, "Product 11", 100, 2)), new BigDecimal("200"), null,
				BigDecimal.ZERO, BigDecimal.ZERO, 100L, "Carrier 100", BigDecimal.ZERO,
				new BigDecimal("200"), List.of());
		return new CheckoutPreviewResponse(address(), PaymentMethod.COD, List.of(), List.of(shop), null,
				new BigDecimal("200"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200"), List.of(), true);
	}

	private static CheckoutLineResponse line(Long id, String name, int price, int quantity) {
		return new CheckoutLineResponse(id, name, "https://example.test/" + id + ".png", "NM",
				BigDecimal.valueOf(price), BigDecimal.valueOf(price), quantity, 10,
				BigDecimal.valueOf((long) price * quantity), null);
	}

	private static AddressResponse address() {
		return new AddressResponse(4L, "Nguyen Van A", "0900000000", "Province", "District", "Ward",
				"1 Main", "1 Main, Ward, District, Province", true, LocalDateTime.now());
	}

	private List<Product> products() {
		return new ArrayList<>(List.of(product(11L, 10, 10L, 90L), product(12L, 10, 10L, 90L),
				product(21L, 10, 20L, 91L), product(22L, 10, 20L, 91L)));
	}

	private static Product product(Long id, int stock, Long shopId, Long ownerId) {
		User owner = buyer();
		owner.setId(ownerId);
		Shop shop = new Shop();
		shop.setId(shopId);
		shop.setName("Shop " + shopId);
		shop.setOwner(owner);
		Product product = new Product();
		product.setId(id);
		product.setName("Product " + id);
		product.setStock(stock);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product.setCardCondition(CardCondition.NEAR_MINT);
		product.setShop(shop);
		return product;
	}

	private static User buyer() {
		User user = new User();
		user.setId(BUYER_ID);
		user.setEmail("buyer@example.test");
		user.setFullName("Nguyen Van A");
		return user;
	}
}
