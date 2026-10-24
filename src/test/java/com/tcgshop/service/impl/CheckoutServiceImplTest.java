package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.request.ShopCheckoutOption;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.dto.response.CartItemResponse;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.dto.response.CartShopGroupResponse;
import com.tcgshop.dto.response.CheckoutShopResponse;
import com.tcgshop.dto.response.CouponApplyResponse;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.AddressService;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CouponService;
import com.tcgshop.service.ShippingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {
	@Mock private CartService cartService;
	@Mock private AddressService addressService;
	@Mock private ShippingService shippingService;
	@Mock private CouponService couponService;
	@Mock private CurrentUser currentUser;

	private CheckoutServiceImpl checkoutService;

	@BeforeEach
	void setUp() {
		checkoutService = new CheckoutServiceImpl(cartService, addressService, shippingService, couponService, currentUser);
		lenient().when(currentUser.getId()).thenReturn(77L);
		lenient().when(addressService.findDefault()).thenReturn(Optional.of(address()));
		lenient().when(shippingService.getActiveCarriers()).thenReturn(List.of(carrier(1L, 30_000)));
		lenient().when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(bd(30_000));
		lenient().when(cartService.getCart()).thenReturn(cart(shop(10L, "Shop A", line(1L, "A", "100000", 2)),
				shop(20L, "Shop B", line(2L, "B", "50000", 1))));
	}

	@Test
	void calculatesOneShopSubtotalAndShippingCorrectly() {
		when(cartService.getCart()).thenReturn(cart(shop(10L, "Shop A",
				line(1L, "Card A", "100000", 2), line(2L, "Card B", "50000", 1))));
		when(shippingService.calculateFee(1L, bd(250_000))).thenReturn(bd(30_000));

		var preview = checkoutService.preview(request(List.of(option(10L, 1L, null)), null));

		assertMoney("250000", preview.itemsSubtotal());
		assertMoney("0", preview.totalDiscount());
		assertMoney("30000", preview.totalShippingFee());
		assertMoney("280000", preview.grandTotal());
		assertTrue(preview.canPlaceOrder());
		assertTotalsInvariant(preview);
	}

	@Test
	void duplicateShopIdsAreRejectedBeforeLoadingCartOrUser() {
		var invalidRequest = request(List.of(option(10L, 1L, null), option(10L, 2L, null)), null);

		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> checkoutService.preview(invalidRequest));

		assertEquals("Dữ liệu thanh toán không hợp lệ", exception.getMessage());
		verify(cartService, never()).getCart();
		verify(currentUser, never()).getId();
	}

	@Test
	void totalForMultipleShopsEqualsSumOfShopTotals() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

		var preview = checkoutService.preview(request(List.of(option(10L, 1L, null), option(20L, 1L, null)), null));

		assertEquals(2, preview.shops().size());
		assertMoney("200000", preview.shops().get(0).subtotal());
		assertMoney("50000", preview.shops().get(1).subtotal());
		assertMoney("250000", preview.grandTotal());
		assertTotalsInvariant(preview);
	}

	@Test
	void roundsUnitPriceHalfUpBeforeMultiplyingQuantity() {
		when(cartService.getCart()).thenReturn(cart(shop(10L, "Shop A", line(1L, "Odd price", "12345.50", 2))));
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

		var preview = checkoutService.preview(request(List.of(option(10L, 1L, null)), null));

		assertMoney("12346", preview.shops().get(0).lines().get(0).unitPrice());
		assertMoney("24692", preview.shops().get(0).lines().get(0).lineTotal());
		assertMoney("24692", preview.grandTotal());
		assertTotalsInvariant(preview);
	}

	@Test
	void shopCouponIsCappedAtSubtotalAndInvalidCouponDoesNotBlockCheckout() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		when(couponService.validateAndCalculate("SAVE", 77L, 10L, bd(100)))
				.thenReturn(new CouponApplyResponse(true, 5L, bd(150), "Đã áp dụng"));
		var capped = checkoutService.preview(request(List.of(option(10L, 1L, " save ")), null,
				cart(shop(10L, "Shop A", line(1L, "A", "100", 1)))));
		assertMoney("100", capped.shops().get(0).discountAmount());
		assertMoney("0", capped.grandTotal());
		assertTrue(capped.canPlaceOrder());

		when(couponService.validateAndCalculate("BAD", 77L, 10L, bd(100)))
				.thenReturn(new CouponApplyResponse(false, null, bd(40), "Mã không hợp lệ"));
		var invalid = checkoutService.preview(request(List.of(option(10L, 1L, " bad ")), null,
				cart(shop(10L, "Shop A", line(1L, "A", "100", 1)))));
		assertFalse(invalid.shops().get(0).shopCoupon().applied());
		assertEquals("Mã không hợp lệ", invalid.shops().get(0).shopCoupon().message());
		assertMoney("0", invalid.totalDiscount());
		assertMoney("100", invalid.grandTotal());
		assertTrue(invalid.canPlaceOrder());
		assertTotalsInvariant(invalid);
	}

	@Test
	void platformCouponUsesLargestRemainderAndCapsAtRemainingShopBase() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		when(cartService.getCart()).thenReturn(cart(shop(10L, "Shop A", line(1L, "A", "250000", 1)),
				shop(20L, "Shop B", line(2L, "B", "100000", 1))));
		when(couponService.validateAndCalculate("PLATFORM", 77L, null, bd(350_000)))
				.thenReturn(new CouponApplyResponse(true, 9L, bd(70_000), "Đã áp dụng"));
		var split = checkoutService.preview(request(List.of(option(10L, 1L, null), option(20L, 1L, null)), " platform "));
		assertMoney("50000", split.shops().get(0).platformCouponShare());
		assertMoney("20000", split.shops().get(1).platformCouponShare());
		assertMoney("70000", split.shops().stream().map(CheckoutShopResponse::platformCouponShare)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		assertMoney("70000", split.totalDiscount());
		assertTotalsInvariant(split);

		when(couponService.validateAndCalculate("PLATFORM", 77L, null, bd(350_000)))
				.thenReturn(new CouponApplyResponse(true, 9L, bd(500_000), "Đã áp dụng"));
		var capped = checkoutService.preview(request(List.of(option(10L, 1L, null), option(20L, 1L, null)), "PLATFORM"));
		assertMoney("350000", capped.totalDiscount());
		assertMoney("0", capped.grandTotal());
		assertTrue(capped.shops().stream().allMatch(shop -> shop.totalAmount().signum() >= 0));
		assertMoney("350000", capped.shops().stream().map(CheckoutShopResponse::platformCouponShare)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		assertTotalsInvariant(capped);
	}

	@Test
	void couponCodesAreNormalizedAndServicesReceiveCorrectScopeAndSubtotal() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		when(couponService.validateAndCalculate(anyString(), anyLong(), nullable(Long.class), any(BigDecimal.class)))
				.thenReturn(new CouponApplyResponse(false, null, BigDecimal.ZERO, "Không hợp lệ"));
		checkoutService.preview(request(List.of(option(10L, 1L, " shop-a "),
				option(20L, 1L, " shop-b ")), " platform "));

		ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Long> userIds = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> shopIds = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<BigDecimal> subtotals = ArgumentCaptor.forClass(BigDecimal.class);
		verify(couponService, times(3)).validateAndCalculate(codes.capture(), userIds.capture(),
				shopIds.capture(), subtotals.capture());
		assertEquals(List.of("SHOP-A", "SHOP-B", "PLATFORM"), codes.getAllValues());
		assertEquals(List.of(77L, 77L, 77L), userIds.getAllValues());
		assertEquals(java.util.Arrays.asList(10L, 20L, null), shopIds.getAllValues());
		assertMoney("200000", subtotals.getAllValues().get(0));
		assertMoney("50000", subtotals.getAllValues().get(1));
		assertMoney("250000", subtotals.getAllValues().get(2));
	}

	@Test
	void resolvesAddressAndPropagatesOwnershipNotFound() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		when(addressService.findDefault()).thenReturn(Optional.empty());
		var missing = checkoutService.preview(request(List.of(option(10L, 1L, null)), null));
		assertTrue(missing.issues().contains("Vui lòng chọn địa chỉ nhận hàng"));
		assertFalse(missing.canPlaceOrder());

		when(addressService.getForCheckout(999L)).thenThrow(new ResourceNotFoundException("Địa chỉ", 999L));
		assertThrows(ResourceNotFoundException.class,
				() -> checkoutService.preview(new CheckoutPreviewRequest(999L,
						List.of(option(10L, 1L, null)), null, PaymentMethod.MOMO)));

		AddressResponse selected = address();
		when(addressService.getForCheckout(3L)).thenReturn(selected);
		var explicit = checkoutService.preview(new CheckoutPreviewRequest(3L,
				List.of(option(10L, 1L, null)), null, PaymentMethod.MOMO));
		assertEquals(selected, explicit.address());
		assertEquals(PaymentMethod.MOMO, explicit.paymentMethod());
		verify(addressService).getForCheckout(3L);
	}

	@Test
	void invalidLinesBlockCheckoutAndNoSelectedLineReturnsZeroTotals() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		CartItemResponse unavailable = line(3L, "Hidden", "40", 1, false, false, "Sản phẩm đã ẩn");
		var blocked = checkoutService.preview(request(List.of(option(10L, 1L, null)), null,
				cart(shop(10L, "Shop A", unavailable))));
		assertEquals("Sản phẩm đã ẩn", blocked.shops().get(0).lines().get(0).problem());
		assertFalse(blocked.canPlaceOrder());
		assertFalse(blocked.shops().get(0).issues().isEmpty());

		CartItemResponse overStock = new CartItemResponse(5L, 5L, "Low stock", null, "Near Mint",
				bd(80), null, 2, 1, bd(160), true, true, null, true, false, null);
		var stockBlocked = checkoutService.preview(request(List.of(option(10L, 1L, null)), null,
				cart(shop(10L, "Shop A", overStock))));
		assertEquals("Chỉ còn 1 sản phẩm", stockBlocked.shops().get(0).lines().get(0).problem());
		assertFalse(stockBlocked.canPlaceOrder());

		when(cartService.getCart()).thenReturn(cart(shop(10L, "Shop A",
				line(4L, "Unselected", "200", 1, false, true, null))));
		var empty = checkoutService.preview(request(List.of(), null));
		assertEquals(List.of("Vui lòng chọn ít nhất một sản phẩm để thanh toán"), empty.issues());
		assertFalse(empty.canPlaceOrder());
		assertMoney("0", empty.itemsSubtotal());
		assertMoney("0", empty.grandTotal());
		assertTrue(empty.shops().isEmpty());
	}

	@Test
	void shippingDefaultsToLowestFeeAndInvalidCarrierAddsShopIssueInsteadOfThrowing() {
		when(shippingService.getActiveCarriers()).thenReturn(List.of(carrier(8L, 1_000), carrier(2L, 500),
				carrier(1L, 500)));
		when(shippingService.calculateFee(1L, bd(200_000))).thenReturn(bd(25_000));
		var selected = checkoutService.preview(request(List.of(option(10L, null, null)), null,
				cart(shop(10L, "Shop A", line(1L, "A", "200000", 1)))));
		assertEquals(1L, selected.shops().get(0).carrierId());
		assertEquals("Carrier 1", selected.shops().get(0).carrierName());
		assertMoney("25000", selected.totalShippingFee());

		when(shippingService.calculateFee(999L, bd(200_000)))
				.thenThrow(new BadRequestException("Carrier không khả dụng"));
		var invalid = checkoutService.preview(request(List.of(option(10L, 999L, null)), null,
				cart(shop(10L, "Shop A", line(1L, "A", "200000", 1)))));
		assertTrue(invalid.shops().get(0).issues().get(0)
				.contains("Cửa hàng Shop A: vui lòng chọn đơn vị vận chuyển hợp lệ"));
		assertNull(invalid.shops().get(0).carrierId());
		assertNull(invalid.shops().get(0).carrierName());
		assertMoney("0", invalid.shops().get(0).shippingFee());
		assertFalse(invalid.canPlaceOrder());
	}

	@Test
	void missingCarriersIsGlobalIssueAndServicesAreCalledOncePerPreview() {
		when(shippingService.getActiveCarriers()).thenReturn(List.of());
		var preview = checkoutService.preview(request(List.of(option(10L, null, null), option(20L, null, null)), null));
		assertTrue(preview.issues().contains("Hiện chưa có đơn vị vận chuyển khả dụng"));
		assertFalse(preview.canPlaceOrder());
		verify(cartService, times(1)).getCart();
		verify(shippingService, times(1)).getActiveCarriers();
		assertTotalsInvariant(preview);
	}

	@Test
	void propertyStylePreviewsPreserveMoneyInvariantsForSeededRandomCarts() {
		Random random = new Random(0x13B5EEDL);
		AtomicReference<CartResponse> cartRef = new AtomicReference<>();
		AtomicReference<Map<Long, BigDecimal>> discountsRef = new AtomicReference<>(Map.of());
		when(cartService.getCart()).thenAnswer(invocation -> cartRef.get());
		when(shippingService.getActiveCarriers()).thenReturn(List.of(carrier(1L, 0)));
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(bd(100));
		when(couponService.validateAndCalculate(anyString(), anyLong(), nullable(Long.class), any(BigDecimal.class)))
				.thenAnswer(invocation -> {
					Long shopId = invocation.getArgument(2);
					BigDecimal requestedDiscount = discountsRef.get().getOrDefault(shopId, BigDecimal.ZERO);
					return new CouponApplyResponse(true, 10L, requestedDiscount, "Applied");
				});

		for (int iteration = 0; iteration < 300; iteration++) {
			int shopCount = 1 + random.nextInt(5);
			List<CartShopGroupResponse> cartShops = new ArrayList<>();
			List<ShopCheckoutOption> options = new ArrayList<>();
			Map<Long, BigDecimal> discounts = new HashMap<>();
			BigDecimal subtotalAll = BigDecimal.ZERO;
			for (int index = 0; index < shopCount; index++) {
				Long shopId = (long) (index + 1);
				int quantity = 1 + random.nextInt(5);
				long unitPrice = 1 + random.nextInt(1_000_000);
				BigDecimal subtotal = bd(unitPrice).multiply(BigDecimal.valueOf(quantity));
				cartShops.add(shop(shopId, "Shop " + shopId, line(shopId, "Product " + shopId,
						Long.toString(unitPrice), quantity)));
				options.add(option(shopId, 1L, "shop" + shopId));
				BigDecimal shopDiscount = bd(random.nextLong(subtotal.longValueExact() + 1));
				discounts.put(shopId, shopDiscount);
				subtotalAll = subtotalAll.add(subtotal);
			}
			discounts.put(null, bd(random.nextLong(subtotalAll.longValueExact() + 1)));
			discountsRef.set(discounts);
			cartRef.set(cart(cartShops.toArray(CartShopGroupResponse[]::new)));

			var preview = checkoutService.preview(request(options, "platform"));
			assertTotalsInvariant(preview);
			assertTrue(preview.shops().stream().allMatch(shop -> shop.totalAmount().signum() >= 0),
					"Tổng shop không được âm tại vòng " + iteration);
			BigDecimal platformShares = preview.shops().stream()
					.map(CheckoutShopResponse::platformCouponShare).reduce(BigDecimal.ZERO, BigDecimal::add);
			assertMoney(preview.platformCoupon().discountAmount().toPlainString(), platformShares);
		}
	}

	@Test
	void getCartAndCarrierListAreEachFetchedOnce() {
		when(shippingService.calculateFee(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
		checkoutService.preview(request(List.of(option(10L, 1L, null), option(20L, 1L, null)), null));
		verify(cartService, times(1)).getCart();
		verify(shippingService, times(1)).getActiveCarriers();
	}

	private CheckoutPreviewRequest request(List<ShopCheckoutOption> options, String platformCode) {
		return new CheckoutPreviewRequest(null, options, platformCode, PaymentMethod.COD);
	}

	private CheckoutPreviewRequest request(List<ShopCheckoutOption> options, String platformCode, CartResponse cart) {
		when(cartService.getCart()).thenReturn(cart);
		return request(options, platformCode);
	}

	private ShopCheckoutOption option(Long shopId, Long carrierId, String code) {
		return new ShopCheckoutOption(shopId, carrierId, code);
	}

	private CartResponse cart(CartShopGroupResponse... shops) {
		return new CartResponse(List.of(shops), 0, 0, BigDecimal.ZERO, false);
	}

	private CartShopGroupResponse shop(Long id, String name, CartItemResponse... items) {
		return new CartShopGroupResponse(id, name, List.of(items), BigDecimal.ZERO, true);
	}

	private CartItemResponse line(Long productId, String name, String unitPrice, int quantity) {
		return line(productId, name, unitPrice, quantity, true, true, null);
	}

	private CartItemResponse line(Long productId, String name, String unitPrice, int quantity,
			boolean selected, boolean available, String unavailableReason) {
		BigDecimal price = new BigDecimal(unitPrice);
		return new CartItemResponse(productId, productId, name, null, "Near Mint", price, null, quantity,
				100, price.multiply(BigDecimal.valueOf(quantity)), selected, available, unavailableReason,
				false, false, null);
	}

	private ShippingCarrierResponse carrier(Long id, long fee) {
		return new ShippingCarrierResponse(id, "Carrier " + id, "C" + id, null, bd(fee), null);
	}

	private AddressResponse address() {
		return new AddressResponse(3L, "Nguyen Van A", "0912345678", "Hanoi", "District 1", "Ward 1",
				"Street 1", "Street 1, Ward 1, District 1, Hanoi", true, LocalDateTime.now());
	}

	private static BigDecimal bd(long value) { return BigDecimal.valueOf(value); }

	private void assertMoney(String expected, BigDecimal actual) {
		assertNotNull(actual);
		assertEquals(0, new BigDecimal(expected).compareTo(actual),
				"Expected " + expected + " but got " + actual.toPlainString());
	}

	private void assertTotalsInvariant(com.tcgshop.dto.response.CheckoutPreviewResponse preview) {
		assertMoney(preview.itemsSubtotal().subtract(preview.totalDiscount())
				.add(preview.totalShippingFee()).toPlainString(), preview.grandTotal());
		assertMoney(preview.shops().stream().map(CheckoutShopResponse::totalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString(), preview.grandTotal());
	}
}
