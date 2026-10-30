package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.PlatformCouponRequest;
import com.tcgshop.dto.request.PlatformPromotionRequest;
import com.tcgshop.dto.request.PlatformPromotionScope;
import com.tcgshop.dto.response.PlatformCouponResponse;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.mapper.PlatformMarketingMapper;
import com.tcgshop.repository.CouponRepository;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformMarketingServiceImplTest {

	@Mock private PromotionRepository promotionRepository;
	@Mock private ShopRepository shopRepository;
	@Mock private GameRepository gameRepository;
	@Mock private SeriesRepository seriesRepository;
	@Mock private ProductRepository productRepository;
	@Mock private CouponRepository couponRepository;
	@Mock private PlatformMarketingMapper marketingMapper;
	@Mock private CurrentUser currentUser;

	private PlatformPromotionServiceImpl promotionService;
	private PlatformCouponAdminServiceImpl couponService;

	@BeforeEach
	void setUp() {
		promotionService = new PlatformPromotionServiceImpl(
				promotionRepository, gameRepository, seriesRepository, productRepository, marketingMapper,
				currentUser, shopRepository);
		couponService = new PlatformCouponAdminServiceImpl(couponRepository, marketingMapper, currentUser);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
	}

	@Test
	void savesShippingPromotionWithPlatformScopeAndAmountDiscount() {
		PlatformPromotionRequest request = new PlatformPromotionRequest();
		request.setTitle("Miễn ship");
		request.setType(PromotionType.SHIPPING_DISCOUNT);
		request.setDiscountAmount(new BigDecimal("20000"));
		request.setMinOrderValue(new BigDecimal("200000"));
		request.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		request.setEndDate(LocalDateTime.of(2026, 1, 31, 23, 59));
		request.setStatus(ActiveStatus.ACTIVE);
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(marketingMapper.toPromotionResponse(any(Promotion.class))).thenReturn(
				new PlatformPromotionResponse(
						1L, "Miễn ship", PromotionType.SHIPPING_DISCOUNT, null, new BigDecimal("20000"),
						new BigDecimal("200000"), null, null, null, null, java.util.List.of(), null,
						request.getStartDate(), request.getEndDate(), ActiveStatus.ACTIVE));

		promotionService.save(null, request);

		org.mockito.ArgumentCaptor<Promotion> captor = org.mockito.ArgumentCaptor.forClass(Promotion.class);
		verify(promotionRepository).save(captor.capture());
		assertNull(captor.getValue().getShop());
		assertEquals(new BigDecimal("20000"), captor.getValue().getDiscountAmount());
		assertEquals(new HashSet<>(), captor.getValue().getProducts());
	}

	@Test
	void savesProductPromotionWithoutScopeAsApplyToAll() {
		PlatformPromotionRequest request = new PlatformPromotionRequest();
		request.setTitle("Giảm sản phẩm");
		request.setType(PromotionType.PRODUCT_DISCOUNT);
		request.setDiscountPercent(new BigDecimal("10"));
		request.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		request.setEndDate(LocalDateTime.of(2026, 1, 31, 23, 59));
		request.setStatus(ActiveStatus.ACTIVE);
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(marketingMapper.toPromotionResponse(any(Promotion.class))).thenReturn(
				new PlatformPromotionResponse(
						1L, request.getTitle(), request.getType(), request.getDiscountPercent(), null, null,
						null, null, null, null, java.util.List.of(), null,
						request.getStartDate(), request.getEndDate(), ActiveStatus.ACTIVE));

		PlatformPromotionResponse response = promotionService.save(null, request);

		org.mockito.ArgumentCaptor<Promotion> captor = org.mockito.ArgumentCaptor.forClass(Promotion.class);
		verify(promotionRepository).save(captor.capture());
		assertNull(captor.getValue().getGame());
		assertNull(captor.getValue().getSeries());
		assertEquals(new HashSet<>(), captor.getValue().getProducts());
		assertEquals("Toàn bộ sản phẩm đang hoạt động", response.targetSummary());
	}

	@Test
	void allScopeOverridesSubmittedTargetIds() {
		PlatformPromotionRequest request = new PlatformPromotionRequest();
		request.setTitle("Giảm toàn sàn");
		request.setType(PromotionType.PRODUCT_DISCOUNT);
		request.setDiscountPercent(new BigDecimal("10"));
		request.setProductIds(java.util.List.of(4L));
		request.setScope(PlatformPromotionScope.ALL);
		request.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		request.setEndDate(LocalDateTime.of(2026, 1, 31, 23, 59));
		request.setStatus(ActiveStatus.ACTIVE);
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(marketingMapper.toPromotionResponse(any(Promotion.class))).thenReturn(
				new PlatformPromotionResponse(
						1L, request.getTitle(), request.getType(), request.getDiscountPercent(), null, null,
						null, null, null, null, java.util.List.of(), null,
						request.getStartDate(), request.getEndDate(), ActiveStatus.ACTIVE));

		promotionService.save(null, request);

		org.mockito.ArgumentCaptor<Promotion> captor = org.mockito.ArgumentCaptor.forClass(Promotion.class);
		verify(promotionRepository).save(captor.capture());
		assertNull(captor.getValue().getShop());
		assertNull(captor.getValue().getGame());
		assertNull(captor.getValue().getSeries());
		assertEquals(new HashSet<>(), captor.getValue().getProducts());
	}

	@Test
	void shopScopeStoresAllActiveShopProductsWithoutSettingPromotionShop() {
		PlatformPromotionRequest request = new PlatformPromotionRequest();
		request.setTitle("Ưu đãi chi nhánh");
		request.setType(PromotionType.PRODUCT_DISCOUNT);
		request.setScope(PlatformPromotionScope.SHOP);
		request.setShopId(42L);
		request.setDiscountPercent(new BigDecimal("10"));
		request.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		request.setEndDate(LocalDateTime.of(2026, 1, 31, 23, 59));
		request.setStatus(ActiveStatus.ACTIVE);

		Shop shop = new Shop();
		shop.setId(42L);
		Product product = new Product();
		product.setId(7L);
		product.setName("Sản phẩm chi nhánh");
		product.setShop(shop);
		when(shopRepository.findApprovedNotDeletedById(42L)).thenReturn(java.util.Optional.of(shop));
		when(productRepository.findAvailableForPromotionOptionsByScope(
				ProductStatus.ACTIVE, null, null, 42L)).thenReturn(java.util.List.of(product));
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(marketingMapper.toPromotionResponse(any(Promotion.class))).thenReturn(
				new PlatformPromotionResponse(
						1L, request.getTitle(), request.getType(), request.getDiscountPercent(), null, null,
						null, null, null, null, java.util.List.of(7L), null,
						request.getStartDate(), request.getEndDate(), ActiveStatus.ACTIVE));

		promotionService.save(null, request);

		org.mockito.ArgumentCaptor<Promotion> captor = org.mockito.ArgumentCaptor.forClass(Promotion.class);
		verify(promotionRepository).save(captor.capture());
		assertNull(captor.getValue().getShop());
		assertEquals(new HashSet<>(java.util.List.of(product)), captor.getValue().getProducts());
	}

	@Test
	void savesCouponAsPlatformAndNormalizesCode() {
		PlatformCouponRequest request = couponRequest("spring10", new BigDecimal("10"), DiscountType.PERCENTAGE);
		PlatformCouponResponse expected = new PlatformCouponResponse(
				1L, "SPRING10", DiscountType.PERCENTAGE, new BigDecimal("10"), BigDecimal.ZERO,
				null, 100, 0, 1, request.getStartDate(), request.getEndDate(), ActiveStatus.ACTIVE);
		when(couponRepository.existsByCodeIgnoreCase("SPRING10")).thenReturn(false);
		when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(marketingMapper.toCouponResponse(any(Coupon.class))).thenReturn(expected);

		assertEquals(expected, couponService.save(null, request));

		org.mockito.ArgumentCaptor<Coupon> captor = org.mockito.ArgumentCaptor.forClass(Coupon.class);
		verify(couponRepository).save(captor.capture());
		assertNull(captor.getValue().getShop());
		assertEquals("SPRING10", captor.getValue().getCode());
	}

	@Test
	void rejectsPercentageCouponAboveOneHundredPercent() {
		PlatformCouponRequest request = couponRequest("TOOMUCH", new BigDecimal("100.01"), DiscountType.PERCENTAGE);

		assertThrows(BadRequestException.class, () -> couponService.save(null, request));

		verify(couponRepository, never()).save(any());
	}

	private PlatformCouponRequest couponRequest(String code, BigDecimal value, DiscountType type) {
		PlatformCouponRequest request = new PlatformCouponRequest();
		request.setCode(code);
		request.setDiscountType(type);
		request.setDiscountValue(value);
		request.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		request.setEndDate(LocalDateTime.of(2026, 1, 31, 23, 59));
		request.setStatus(ActiveStatus.ACTIVE);
		return request;
	}
}
