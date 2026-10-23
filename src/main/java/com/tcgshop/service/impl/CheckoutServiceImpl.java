package com.tcgshop.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.request.ShopCheckoutOption;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.dto.response.CartItemResponse;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.dto.response.CartShopGroupResponse;
import com.tcgshop.dto.response.CheckoutCouponResponse;
import com.tcgshop.dto.response.CheckoutLineResponse;
import com.tcgshop.dto.response.CheckoutPreviewResponse;
import com.tcgshop.dto.response.CheckoutShopResponse;
import com.tcgshop.dto.response.CouponApplyResponse;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.AddressService;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CheckoutService;
import com.tcgshop.service.CouponService;
import com.tcgshop.service.ShippingService;
import com.tcgshop.util.MoneyAllocator;
import com.tcgshop.util.MoneyUtils;

@Service
public class CheckoutServiceImpl implements CheckoutService {
	private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String SELECT_ITEM_ISSUE = "Vui lòng chọn ít nhất một sản phẩm để thanh toán";
	private static final String ADDRESS_ISSUE = "Vui lòng chọn địa chỉ nhận hàng";
	private static final String NO_CARRIER_ISSUE = "Hiện chưa có đơn vị vận chuyển khả dụng";

	private final CartService cartService;
	private final AddressService addressService;
	private final ShippingService shippingService;
	private final CouponService couponService;
	private final CurrentUser currentUser;

	public CheckoutServiceImpl(CartService cartService, AddressService addressService,
			ShippingService shippingService, CouponService couponService, CurrentUser currentUser) {
		this.cartService = cartService;
		this.addressService = addressService;
		this.shippingService = shippingService;
		this.couponService = couponService;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public CheckoutPreviewResponse preview(CheckoutPreviewRequest request) {
		if (request == null || hasDuplicateShopIds(request.shops())) {
			throw new BadRequestException("Dữ liệu thanh toán không hợp lệ");
		}
		Long userId = currentUser.getId();
		CartResponse cart = cartService.getCart();
		List<String> globalIssues = new ArrayList<>();
		List<CartShopGroupResponse> cartShops = cart == null || cart.shops() == null ? List.of() : cart.shops();
		Map<Long, PreviewShop> shopsById = new LinkedHashMap<>();
		int selectedLineCount = collectSelectedLines(cartShops, shopsById);

		if (selectedLineCount == 0) {
			globalIssues.add(SELECT_ITEM_ISSUE);
			return emptyPreview(request, globalIssues);
		}

		AddressResponse address = resolveAddress(request, globalIssues);
		List<ShippingCarrierResponse> carriers = safeList(shippingService.getActiveCarriers());
		if (carriers.isEmpty()) {
			globalIssues.add(NO_CARRIER_ISSUE);
		}

		BigDecimal itemsSubtotal = shopsById.values().stream()
				.map(shop -> shop.subtotal).reduce(ZERO, BigDecimal::add);
			for (PreviewShop shop : shopsById.values()) {
				applyShopCoupon(shop, findOption(request, shop.shopId), userId);
		}

		BigDecimal baseTotal = shopsById.values().stream()
				.map(shop -> shop.baseAfterShopCoupon).reduce(ZERO, BigDecimal::add);
		CheckoutCouponResponse platformCoupon = applyPlatformCoupon(request, userId, itemsSubtotal, baseTotal);

		List<BigDecimal> platformShares = platformCoupon != null && platformCoupon.applied()
				? MoneyAllocator.allocateProportionally(platformCoupon.discountAmount(),
						shopsById.values().stream().map(shop -> shop.baseAfterShopCoupon).toList())
				: zeroShares(shopsById.size());

		List<CheckoutShopResponse> shopResponses = new ArrayList<>(shopsById.size());
		BigDecimal totalDiscount = ZERO;
		BigDecimal totalShippingFee = ZERO;
		BigDecimal grandTotal = ZERO;
		int index = 0;
		for (PreviewShop shop : shopsById.values()) {
			shop.platformCouponShare = platformShares.get(index++);
			shop.discountAmount = shop.shopDiscount.add(shop.platformCouponShare);
			calculateShipping(shop, findOption(request, shop.shopId), carriers);
			shop.totalAmount = shop.subtotal.subtract(shop.discountAmount).add(shop.shippingFee);
			if (shop.totalAmount.compareTo(shop.shippingFee) < 0 || shop.shippingFee.signum() < 0) {
				throw new IllegalStateException("Checkout total không thể thấp hơn phí vận chuyển");
			}
			totalDiscount = totalDiscount.add(shop.discountAmount);
			totalShippingFee = totalShippingFee.add(shop.shippingFee);
			grandTotal = grandTotal.add(shop.totalAmount);
			shopResponses.add(shop.toResponse());
		}

		BigDecimal expectedGrandTotal = itemsSubtotal.subtract(totalDiscount).add(totalShippingFee);
		if (grandTotal.compareTo(expectedGrandTotal) != 0) {
			throw new IllegalStateException("Tổng checkout không khớp subtotal - discount + shipping");
		}
		boolean canPlaceOrder = globalIssues.isEmpty()
				&& shopsById.values().stream().allMatch(shop -> shop.issues.isEmpty());
		log.debug("Checkout preview calculated for {} shops and {} selected lines", shopsById.size(), selectedLineCount);
		return new CheckoutPreviewResponse(address, request.paymentMethod(), carriers, shopResponses,
				platformCoupon, itemsSubtotal, totalDiscount, totalShippingFee, grandTotal,
					globalIssues, canPlaceOrder);
	}

	private boolean hasDuplicateShopIds(List<ShopCheckoutOption> options) {
		if (options == null) {
			return false;
		}
		Set<Long> seenShopIds = new HashSet<>();
		return options.stream().filter(Objects::nonNull).map(ShopCheckoutOption::shopId)
				.filter(Objects::nonNull).anyMatch(shopId -> !seenShopIds.add(shopId));
	}

	private int collectSelectedLines(List<CartShopGroupResponse> cartShops,
			Map<Long, PreviewShop> shopsById) {
		int selectedLineCount = 0;
		for (CartShopGroupResponse cartShop : cartShops) {
			if (cartShop == null || cartShop.items() == null) {
				continue;
			}
			for (CartItemResponse item : cartShop.items()) {
				if (item == null || !item.selected()) {
					continue;
				}
				selectedLineCount++;
				PreviewShop shop = shopsById.computeIfAbsent(cartShop.shopId(),
						ignored -> new PreviewShop(cartShop.shopId(), cartShop.shopName()));
				BigDecimal unitPrice = MoneyUtils.round(item.unitPrice());
				BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
				String problem = null;
				if (!item.available()) {
					problem = item.unavailableReason() == null || item.unavailableReason().isBlank()
							? "Sản phẩm không còn khả dụng" : item.unavailableReason();
				} else if (item.quantityExceedsStock()) {
					problem = "Chỉ còn " + (item.stock() == null ? 0 : item.stock()) + " sản phẩm";
				}
				if (problem != null) {
					shop.issues.add(item.productName() + ": " + problem);
				}
				shop.lines.add(new CheckoutLineResponse(item.productId(), item.productName(), item.imageUrl(),
						item.cardCondition(), unitPrice, roundNullable(item.originalPrice()), item.quantity(),
						item.stock() == null ? 0 : item.stock(), lineTotal, problem));
				shop.subtotal = shop.subtotal.add(lineTotal);
			}
		}
		return selectedLineCount;
	}

	private AddressResponse resolveAddress(CheckoutPreviewRequest request, List<String> globalIssues) {
		if (request.addressId() != null) {
			return addressService.getForCheckout(request.addressId());
		}
		Optional<AddressResponse> defaultAddress = addressService.findDefault();
		if (defaultAddress.isEmpty()) {
			globalIssues.add(ADDRESS_ISSUE);
			return null;
		}
		return defaultAddress.get();
	}

	private void applyShopCoupon(PreviewShop shop, ShopCheckoutOption option, Long userId) {
		String code = normalizeCode(option == null ? null : option.couponCode());
		if (code == null) {
			shop.baseAfterShopCoupon = shop.subtotal;
			return;
		}
		CouponApplyResponse result = couponService.validateAndCalculate(code, userId, shop.shopId, shop.subtotal);
		if (result != null && result.valid()) {
			shop.shopDiscount = clampDiscount(result.discountAmount(), shop.subtotal);
			shop.baseAfterShopCoupon = shop.subtotal.subtract(shop.shopDiscount);
			shop.shopCoupon = new CheckoutCouponResponse(code, true, result.couponId(), shop.shopDiscount,
					result.message());
		} else {
			shop.baseAfterShopCoupon = shop.subtotal;
			shop.shopCoupon = new CheckoutCouponResponse(code, false,
					result == null ? null : result.couponId(), ZERO,
					result == null ? "Không thể kiểm tra mã giảm giá" : result.message());
		}
	}

	private CheckoutCouponResponse applyPlatformCoupon(CheckoutPreviewRequest request, Long userId,
			BigDecimal itemsSubtotal, BigDecimal baseTotal) {
		String code = normalizeCode(request.platformCouponCode());
		if (code == null) {
			return null;
		}
		CouponApplyResponse result = couponService.validateAndCalculate(code, userId, null, itemsSubtotal);
		if (result == null || !result.valid()) {
			return new CheckoutCouponResponse(code, false, result == null ? null : result.couponId(), ZERO,
					result == null ? "Không thể kiểm tra mã giảm giá" : result.message());
		}
		BigDecimal discount = clampDiscount(result.discountAmount(), baseTotal);
		return new CheckoutCouponResponse(code, true, result.couponId(), discount, result.message());
	}

	private void calculateShipping(PreviewShop shop, ShopCheckoutOption option,
			List<ShippingCarrierResponse> carriers) {
		Long carrierId = option == null ? null : option.carrierId();
		if (carrierId == null && !carriers.isEmpty()) {
			carrierId = carriers.stream().min(Comparator
					.comparing((ShippingCarrierResponse carrier) -> MoneyUtils.nonNegative(carrier.baseFee()))
					.thenComparing(ShippingCarrierResponse::id, Comparator.nullsLast(Long::compareTo)))
					.map(ShippingCarrierResponse::id).orElse(null);
		}
		if (carrierId == null) {
			shop.shippingFee = ZERO;
			return;
		}

		Long selectedCarrierId = carrierId;
		try {
			// TODO(PA-19): Khuyến mãi giảm phí ship của sàn được xử lý bên trong ShippingService.
			shop.shippingFee = MoneyUtils.nonNegative(MoneyUtils.round(
					shippingService.calculateFee(selectedCarrierId, shop.subtotal)));
			shop.carrierId = selectedCarrierId;
			shop.carrierName = carriers.stream().filter(carrier -> selectedCarrierId.equals(carrier.id()))
					.map(ShippingCarrierResponse::name).findFirst().orElse(null);
		} catch (BadRequestException exception) {
			shop.issues.add("Cửa hàng " + shop.shopName + ": vui lòng chọn đơn vị vận chuyển hợp lệ");
			shop.shippingFee = ZERO;
			shop.carrierId = null;
			shop.carrierName = null;
		}
	}

	private CheckoutPreviewResponse emptyPreview(CheckoutPreviewRequest request, List<String> issues) {
		return new CheckoutPreviewResponse(null, request.paymentMethod(), List.of(), List.of(), null,
				ZERO, ZERO, ZERO, ZERO, issues, false);
	}

	private ShopCheckoutOption findOption(CheckoutPreviewRequest request, Long shopId) {
		if (request.shops() == null) {
			return null;
		}
		return request.shops().stream().filter(option -> option != null && java.util.Objects.equals(shopId, option.shopId()))
				.findFirst().orElse(null);
	}

	private String normalizeCode(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		String normalized = code.trim().toUpperCase(Locale.ROOT);
		return normalized.isEmpty() ? null : normalized;
	}

	private BigDecimal clampDiscount(BigDecimal discount, BigDecimal maximum) {
		BigDecimal nonNegative = MoneyUtils.nonNegative(MoneyUtils.round(discount));
		BigDecimal limit = MoneyUtils.nonNegative(MoneyUtils.round(maximum));
		return nonNegative.min(limit).setScale(0);
	}

	private BigDecimal roundNullable(BigDecimal amount) {
		return amount == null ? null : MoneyUtils.round(amount);
	}

	private List<ShippingCarrierResponse> safeList(List<ShippingCarrierResponse> values) {
		return values == null ? List.of() : values;
	}

	private List<BigDecimal> zeroShares(int count) {
		List<BigDecimal> values = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			values.add(ZERO);
		}
		return List.copyOf(values);
	}

	private static final class PreviewShop {
		private final Long shopId;
		private final String shopName;
		private final List<CheckoutLineResponse> lines = new ArrayList<>();
		private final List<String> issues = new ArrayList<>();
		private BigDecimal subtotal = ZERO;
		private BigDecimal shopDiscount = ZERO;
		private BigDecimal baseAfterShopCoupon = ZERO;
		private BigDecimal platformCouponShare = ZERO;
		private BigDecimal discountAmount = ZERO;
		private Long carrierId;
		private String carrierName;
		private BigDecimal shippingFee = ZERO;
		private BigDecimal totalAmount = ZERO;
		private CheckoutCouponResponse shopCoupon;

		private PreviewShop(Long shopId, String shopName) {
			this.shopId = shopId;
			this.shopName = shopName;
		}

		private CheckoutShopResponse toResponse() {
			return new CheckoutShopResponse(shopId, shopName, lines, subtotal, shopCoupon, platformCouponShare,
					discountAmount, carrierId, carrierName, shippingFee, totalAmount, issues);
		}
	}
}
