package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PlaceOrderRequest;
import com.tcgshop.dto.request.ShopCheckoutOption;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.dto.response.CheckoutCouponResponse;
import com.tcgshop.dto.response.CheckoutLineResponse;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.CheckoutShopResponse;
import com.tcgshop.dto.response.PlaceOrderResponse;
import com.tcgshop.dto.response.PlacedOrderSummaryResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.User;
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
import com.tcgshop.service.OrderPlacementService;
import com.tcgshop.util.OrderCodeGenerator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementServiceImpl implements OrderPlacementService {
	private static final Logger log = LoggerFactory.getLogger(OrderPlacementServiceImpl.class);
	private static final String REDIRECT_PREFIX = "/checkout/success/";
	private static final String PAYMENT_SESSION_ERROR = "Phiên thanh toán không hợp lệ, vui lòng tải lại trang";
	private static final String TOTAL_CHANGED_ERROR = "Giá hoặc ưu đãi đã thay đổi, vui lòng kiểm tra lại đơn hàng trước khi đặt";
	private static final String CARRIER_REQUIRED_ERROR = "Vui lòng chọn đơn vị vận chuyển cho từng cửa hàng";
	private static final String EMPTY_CHECKOUT_ERROR = "Vui lòng chọn sản phẩm để thanh toán";
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final CurrentUser currentUser;
	private final UserRepository userRepository;
	private final OrderGroupRepository orderGroupRepository;
	private final OrderRepository orderRepository;
	private final OrderStatusHistoryRepository orderStatusHistoryRepository;
	private final ProductRepository productRepository;
	private final ShippingCarrierRepository shippingCarrierRepository;
	private final CheckoutService checkoutService;
	private final CartService cartService;
	private final CouponService couponService;
	private final OrderCodeGenerator orderCodeGenerator;
	private final ApplicationEventPublisher eventPublisher;

	public OrderPlacementServiceImpl(CurrentUser currentUser, UserRepository userRepository,
			OrderGroupRepository orderGroupRepository, OrderRepository orderRepository,
			OrderStatusHistoryRepository orderStatusHistoryRepository, ProductRepository productRepository,
			ShippingCarrierRepository shippingCarrierRepository, CheckoutService checkoutService,
			CartService cartService, CouponService couponService,
			OrderCodeGenerator orderCodeGenerator, ApplicationEventPublisher eventPublisher) {
		this.currentUser = currentUser;
		this.userRepository = userRepository;
		this.orderGroupRepository = orderGroupRepository;
		this.orderRepository = orderRepository;
		this.orderStatusHistoryRepository = orderStatusHistoryRepository;
		this.productRepository = productRepository;
		this.shippingCarrierRepository = shippingCarrierRepository;
		this.checkoutService = checkoutService;
		this.cartService = cartService;
		this.couponService = couponService;
		this.orderCodeGenerator = orderCodeGenerator;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
		Long buyerId = currentUser.getId();
		// Bước 1: khóa user ngay từ đầu để tuần tự hóa các lần đặt hàng đồng thời của cùng một người.
		// Khóa này cũng giữ cho lần kiểm tra idempotency ở bước kế tiếp không bị race giữa hai checkout.
		User buyer = userRepository.findByIdForUpdate(buyerId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", buyerId));

		String idempotencyToken = request.idempotencyToken().toLowerCase(Locale.ROOT);
		// Bước 2: chỉ chủ sở hữu token mới được nhận lại kết quả cũ; không chạy lại preview hay nghiệp vụ.
		// Token thuộc người khác là dấu hiệu request/session bị tráo, vì vậy từ chối trước mọi thay đổi.
		OrderGroup previousGroup = orderGroupRepository.findByIdempotencyToken(idempotencyToken).orElse(null);
		if (previousGroup != null) {
			if (previousGroup.getUser() == null || !buyerId.equals(previousGroup.getUser().getId())) {
				throw new BadRequestException(PAYMENT_SESSION_ERROR);
			}
			return toResponse(previousGroup,
					orderRepository.findByOrderGroupIdOrderByIdAsc(previousGroup.getId()), true);
		}

		if (request.paymentMethod() != PaymentMethod.COD && request.paymentMethod() != PaymentMethod.MOMO) {
			throw new BusinessException("Phương thức thanh toán không hợp lệ");
		}

		CheckoutPreviewResponse preview = checkoutService.preview(request.toPreviewRequest());
		// Bước 4: preview là nguồn tính tiền ở server; nếu không thể đặt, trả issue đầu tiên để người mua sửa.
		if (!preview.canPlaceOrder()) {
			throw new BusinessException(firstIssue(preview));
		}
		// Bước 5: preview cho phép hiển thị mã sai nhưng đặt hàng phải từ chối, không được âm thầm bỏ ưu đãi.
		validateRequestedCoupons(request, preview);
		// Bước 6: bắt người mua chốt carrier cho từng shop và đảm bảo carrier đó chính là carrier đã tính phí.
		validateCarriers(request, preview);
		// Bước 7: tổng client chỉ là optimistic check; tuyệt đối không dùng nó để ghi tiền vào Order/OrderGroup.
		if (request.expectedGrandTotal().compareTo(preview.grandTotal()) != 0) {
			throw new ConflictException(TOTAL_CHANGED_ERROR);
		}

		List<PlacedLine> lines = flattenLines(preview);
		Set<Long> productIds = new LinkedHashSet<>();
		lines.forEach(line -> productIds.add(line.productId()));
		// Bước 8: nạp toàn bộ sản phẩm + shop + chủ shop trong một truy vấn để kiểm tra ownership và snapshot.
		List<Product> fetchedProducts = productIds.isEmpty()
				? List.of() : productRepository.findAllWithShopOwnerByIdIn(productIds);
		Map<Long, Product> productsById = new LinkedHashMap<>();
		for (Product product : fetchedProducts) {
			productsById.put(product.getId(), product);
			if (product.getShop() != null && product.getShop().getOwner() != null
					&& buyerId.equals(product.getShop().getOwner().getId())) {
				throw new BadRequestException("Bạn không thể mua sản phẩm của cửa hàng của chính mình");
			}
		}
		for (PlacedLine line : lines) {
			if (!productsById.containsKey(line.productId())) {
				throw new BusinessException("Sản phẩm '" + line.productName() + "' không còn bán");
			}
		}

		// Bước 9: khóa tồn kho theo thứ tự productId tăng dần để hai checkout nhiều sản phẩm không deadlock.
		// Mỗi UPDATE tự kiểm tra tồn kho, trạng thái và soft-delete; nếu dòng nào thất bại, exception làm rollback cả lô.
		for (PlacedLine line : lines.stream().sorted(Comparator.comparing(PlacedLine::productId)).toList()) {
			int updated = productRepository.decreaseStock(line.productId(), line.quantity(), ProductStatus.ACTIVE);
			if (updated == 0) {
				throw stockFailure(line, productRepository.findById(line.productId()).orElse(null));
			}
		}

		String groupCode = orderCodeGenerator.generateUnique("GRP", orderGroupRepository::existsByGroupCode);
		// Bước 10: chỉ dùng số tiền server tính; tạo group trước để các đơn con tham chiếu được khóa nhóm đã flush.
		OrderGroup group = new OrderGroup();
		group.setGroupCode(groupCode);
		group.setUser(buyer);
		group.setTotalAmount(preview.grandTotal());
		group.setPaymentMethod(request.paymentMethod());
		group.setPaymentStatus(PaymentStatus.PENDING);
		group.setIdempotencyToken(idempotencyToken);
		group = orderGroupRepository.saveAndFlush(group);

		List<Order> orders = new ArrayList<>(preview.shops().size());
		Map<Long, Order> ordersByShopId = new LinkedHashMap<>();
		boolean platformCouponApplied = preview.platformCoupon() != null && preview.platformCoupon().applied();
		for (CheckoutShopResponse shopPreview : preview.shops()) {
			Order order = createOrder(shopPreview, group, buyer, request, preview.address(), productsById,
					platformCouponApplied);
			order = orderRepository.saveAndFlush(order);
			OrderStatusHistory history = new OrderStatusHistory();
			history.setOrder(order);
			history.setPreviousStatus(null);
			history.setNewStatus(OrderStatus.NEW);
			history.setChangedBy(buyer);
			history.setNote("Đặt hàng");
			orderStatusHistoryRepository.save(history);
			orders.add(order);
			ordersByShopId.put(shopPreview.shopId(), order);
		}

		// Bước 11: ghi nhận từng mã shop trên đúng đơn shop; mã sàn chỉ tiêu thụ một lượt cho cả payment group.
		// orderId mã sàn được gắn vào đơn con đầu tiên theo thứ tự preview, còn số giảm là tổng các share đã chia.
		for (CheckoutShopResponse shopPreview : preview.shops()) {
			CheckoutCouponResponse coupon = shopPreview.shopCoupon();
			if (coupon != null && coupon.applied()) {
				couponService.recordUsage(coupon.couponId(), buyerId,
						ordersByShopId.get(shopPreview.shopId()).getId(), coupon.discountAmount());
			}
		}
		CheckoutCouponResponse platformCoupon = preview.platformCoupon();
		if (platformCoupon != null && platformCoupon.applied() && !orders.isEmpty()) {
			BigDecimal allocatedDiscount = preview.shops().stream()
					.map(CheckoutShopResponse::platformCouponShare)
					.filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
			couponService.recordUsage(platformCoupon.couponId(), buyerId, orders.get(0).getId(), allocatedDiscount);
		}

		// Bước 12: chỉ xóa các dòng đã đặt thành công, giữ nguyên mọi sản phẩm khác trong giỏ người mua.
		cartService.removePurchasedItems(productIds);
		// Bước 13: phát sau khi đơn, lịch sử, coupon và giỏ đã được cập nhật; listener chỉ chạy AFTER_COMMIT.
		List<String> orderCodes = new ArrayList<>(orders.size());
		for (Order order : orders) {
			Product firstProduct = productForShop(order.getShop().getId(), preview, productsById);
			Long shopOwnerId = firstProduct.getShop().getOwner() == null
					? null : firstProduct.getShop().getOwner().getId();
			orderCodes.add(order.getOrderCode());
			eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getOrderCode(), group.getId(),
					buyerId, order.getShop().getId(), shopOwnerId));
		}
		eventPublisher.publishEvent(new OrderGroupPlacedEvent(group.getId(), groupCode, buyerId,
				buyer.getEmail(), buyer.getFullName(), request.paymentMethod(), preview.grandTotal(), orderCodes));

		// Chỉ ghi thông tin vận hành cần thiết; tuyệt đối không ghi thông tin liên hệ/địa chỉ hoặc coupon.
		log.info("Order group placed: orderGroupId={}, childOrderCount={}, grandTotal={}",
				group.getId(), orders.size(), preview.grandTotal());
		return toResponse(group, orders, false);
	}

	@Override
	@Transactional(readOnly = true)
	public PlaceOrderResponse getPlacedSummary(String groupCode) {
		Long buyerId = currentUser.getId();
		OrderGroup group = orderGroupRepository.findByGroupCodeAndUserId(groupCode, buyerId)
				.orElseThrow(() -> new ResourceNotFoundException("Nhóm đơn hàng", groupCode));
		return toResponse(group, orderRepository.findByOrderGroupIdOrderByIdAsc(group.getId()), false);
	}

	private void validateRequestedCoupons(PlaceOrderRequest request, CheckoutPreviewResponse preview) {
		if (request.shops() != null) {
			for (ShopCheckoutOption option : request.shops()) {
				if (option == null || !hasText(option.couponCode())) {
					continue;
				}
				CheckoutShopResponse shop = preview.shops().stream()
						.filter(value -> Objects.equals(value.shopId(), option.shopId())).findFirst().orElse(null);
				if (shop == null || shop.shopCoupon() == null || !shop.shopCoupon().applied()) {
					throw new BusinessException(couponMessage(shop == null ? null : shop.shopCoupon()));
				}
			}
		}
		if (hasText(request.platformCouponCode())
				&& (preview.platformCoupon() == null || !preview.platformCoupon().applied())) {
			throw new BusinessException(couponMessage(preview.platformCoupon()));
		}
	}

	private void validateCarriers(PlaceOrderRequest request, CheckoutPreviewResponse preview) {
		for (CheckoutShopResponse shop : preview.shops()) {
			ShopCheckoutOption option = findOption(request, shop.shopId());
			if (option == null || option.carrierId() == null) {
				throw new BadRequestException(CARRIER_REQUIRED_ERROR);
			}
			if (!Objects.equals(option.carrierId(), shop.carrierId())) {
				throw new BadRequestException("Đơn vị vận chuyển đã thay đổi, vui lòng kiểm tra lại");
			}
		}
	}

	private String firstIssue(CheckoutPreviewResponse preview) {
		if (preview.issues() != null && !preview.issues().isEmpty()) {
			return preview.issues().get(0);
		}
		for (CheckoutShopResponse shop : preview.shops()) {
			if (shop.issues() != null && !shop.issues().isEmpty()) {
				return shop.issues().get(0);
			}
		}
		return EMPTY_CHECKOUT_ERROR;
	}

	private String couponMessage(CheckoutCouponResponse coupon) {
		return coupon == null || coupon.message() == null || coupon.message().isBlank()
				? "Mã giảm giá không hợp lệ" : coupon.message();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private ShopCheckoutOption findOption(PlaceOrderRequest request, Long shopId) {
		if (request.shops() == null) {
			return null;
		}
		return request.shops().stream().filter(option -> option != null && Objects.equals(shopId, option.shopId()))
				.findFirst().orElse(null);
	}

	private List<PlacedLine> flattenLines(CheckoutPreviewResponse preview) {
		List<PlacedLine> lines = new ArrayList<>();
		for (CheckoutShopResponse shop : preview.shops()) {
			for (CheckoutLineResponse line : shop.lines()) {
				lines.add(new PlacedLine(shop.shopId(), line.productId(), line.productName(), line.quantity()));
			}
		}
		return lines;
	}

	private BusinessException stockFailure(PlacedLine line, Product product) {
		String productName = product == null || product.getName() == null ? line.productName() : product.getName();
		if (product == null || product.getStatus() != ProductStatus.ACTIVE || !Boolean.FALSE.equals(product.getDeleted())) {
			return new BusinessException("Sản phẩm '" + productName + "' không còn bán");
		}
		int stock = product.getStock() == null ? 0 : product.getStock();
		return stock == 0
				? new BusinessException("Sản phẩm '" + productName + "' đã hết hàng")
				: new BusinessException("Sản phẩm '" + productName + "' chỉ còn " + stock + " sản phẩm");
	}

	private Order createOrder(CheckoutShopResponse shopPreview, OrderGroup group, User buyer,
			PlaceOrderRequest request, AddressResponse address, Map<Long, Product> productsById,
			boolean platformCouponApplied) {
		Order order = new Order();
		order.setOrderCode(orderCodeGenerator.generateUnique("TCG", orderRepository::existsByOrderCode));
		order.setOrderGroup(group);
		order.setUser(buyer);
		Product firstProduct = productsById.get(shopPreview.lines().get(0).productId());
		order.setShop(firstProduct.getShop());
		order.setShippingCarrier(shippingCarrierRepository.getReferenceById(shopPreview.carrierId()));
		order.setStatus(OrderStatus.NEW);
		order.setSubtotal(shopPreview.subtotal());
		order.setShippingFee(shopPreview.shippingFee());
		order.setDiscountAmount(shopPreview.discountAmount());
		order.setPlatformDiscountAmount(!platformCouponApplied || shopPreview.platformCouponShare() == null
				? ZERO : shopPreview.platformCouponShare());
		order.setTotalAmount(shopPreview.totalAmount());
		order.setReceiverName(address.receiverName());
		order.setReceiverPhone(address.phoneNumber());
		order.setShippingAddress(address.fullAddress());
		String note = request.note() == null ? null : request.note().trim();
		order.setNote(note == null || note.isEmpty() ? null : note);
		order.setCodCollected(false);
		order.setCommissionRate(null);
		order.setCommissionAmount(null);
		order.setShopRevenue(null);
		order.setDeliveredAt(null);
		for (CheckoutLineResponse line : shopPreview.lines()) {
			Product product = productsById.get(line.productId());
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setProduct(product);
			item.setProductName(line.productName());
			item.setProductImage(line.imageUrl());
			item.setCardCondition(product.getCardCondition());
			item.setPrice(line.unitPrice());
			item.setQuantity(line.quantity());
			item.setTotalPrice(line.lineTotal());
			order.getItems().add(item);
		}
		return order;
	}

	private Product productForShop(Long shopId, CheckoutPreviewResponse preview, Map<Long, Product> productsById) {
		for (CheckoutShopResponse shop : preview.shops()) {
			if (Objects.equals(shopId, shop.shopId()) && !shop.lines().isEmpty()) {
				return productsById.get(shop.lines().get(0).productId());
			}
		}
		throw new IllegalStateException("Shop trong preview không có dòng sản phẩm");
	}

	private PlaceOrderResponse toResponse(OrderGroup group, List<Order> orders, boolean alreadyPlaced) {
		List<PlacedOrderSummaryResponse> summaries = orders.stream()
				.map(order -> new PlacedOrderSummaryResponse(order.getId(), order.getOrderCode(),
						order.getShop().getId(), order.getShop().getName(), order.getTotalAmount()))
				.toList();
		return new PlaceOrderResponse(group.getId(), group.getGroupCode(), group.getPaymentMethod(),
				group.getTotalAmount(), summaries, REDIRECT_PREFIX + group.getGroupCode(), alreadyPlaced);
	}

	private record PlacedLine(Long shopId, Long productId, String productName, int quantity) {
	}
}
