package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.repository.PromotionPricingProductRepository;
import com.tcgshop.repository.PromotionRepository;
import com.tcgshop.repository.VendorPromotionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

	@Mock private PromotionPricingProductRepository productRepository;
	@Mock private VendorPromotionRepository vendorPromotionRepository;
	@Mock private PromotionRepository platformPromotionRepository;

	private PromotionServiceImpl promotionService;
	private Promotion globalPromotion;
	private Product activeProduct;

	@BeforeEach
	void setUp() {
		promotionService = new PromotionServiceImpl(
				productRepository, vendorPromotionRepository, platformPromotionRepository);
		globalPromotion = new Promotion();
		globalPromotion.setType(PromotionType.PRODUCT_DISCOUNT);
		globalPromotion.setStatus(ActiveStatus.ACTIVE);
		globalPromotion.setDiscountPercent(new BigDecimal("10"));
		activeProduct = new Product();
		activeProduct.setId(7L);
		activeProduct.setStatus(ProductStatus.ACTIVE);
		activeProduct.setPrice(new BigDecimal("1000"));
	}

	@Test
	void appliesTargetlessPlatformDiscountToActiveProducts() {
		when(productRepository.findAllForPromotionPricing(List.of(7L))).thenReturn(List.of(activeProduct));
		when(vendorPromotionRepository.findActiveShopPromotionsForProducts(
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(List.of(7L))))
				.thenReturn(List.of());
		when(platformPromotionRepository.findActivePlatformPromotions(
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of(globalPromotion));

		assertEquals(0, new BigDecimal("900.00").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void doesNotApplyTargetlessPromotionToInactiveProducts() {
		activeProduct.setStatus(ProductStatus.INACTIVE);
		when(productRepository.findAllForPromotionPricing(List.of(7L))).thenReturn(List.of(activeProduct));
		when(vendorPromotionRepository.findActiveShopPromotionsForProducts(
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(List.of(7L))))
				.thenReturn(List.of());
		when(platformPromotionRepository.findActivePlatformPromotions(
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of(globalPromotion));

		assertEquals(0, new BigDecimal("1000").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void explicitProductPromotionDoesNotBecomeGlobal() {
		Product targetedProduct = new Product();
		targetedProduct.setId(8L);
		globalPromotion.getProducts().add(targetedProduct);
		when(productRepository.findAllForPromotionPricing(List.of(7L))).thenReturn(List.of(activeProduct));
		when(vendorPromotionRepository.findActiveShopPromotionsForProducts(
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(List.of(7L))))
				.thenReturn(List.of());
		when(platformPromotionRepository.findActivePlatformPromotions(
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of(globalPromotion));

		assertEquals(0, new BigDecimal("1000").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void explicitPlatformProductTargetReceivesDiscountedEffectivePrice() {
		Product targetedProduct = new Product();
		targetedProduct.setId(7L);
		globalPromotion.getProducts().add(targetedProduct);
		when(productRepository.findAllForPromotionPricing(List.of(7L))).thenReturn(List.of(activeProduct));
		when(vendorPromotionRepository.findActiveShopPromotionsForProducts(
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(List.of(7L))))
				.thenReturn(List.of());
		when(platformPromotionRepository.findActivePlatformPromotions(
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of(globalPromotion));

		assertEquals(0, new BigDecimal("900.00").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void choosesBestSingleDiscountInsteadOfStackingShopAndPlatformPromotions() {
		Shop shop = new Shop();
		shop.setId(3L);
		activeProduct.setShop(shop);

		Promotion shopPromotion = new Promotion();
		shopPromotion.setShop(shop);
		shopPromotion.setDiscountPercent(new BigDecimal("20"));
		shopPromotion.getProducts().add(activeProduct);
		globalPromotion.setDiscountPercent(new BigDecimal("20"));
		stubActivePromotions(List.of(shopPromotion), List.of(globalPromotion));

		assertEquals(0, new BigDecimal("800.00").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void selectsLowerPriceWhenShopAndPlatformDiscountRatesDiffer() {
		Shop shop = new Shop();
		shop.setId(3L);
		activeProduct.setShop(shop);

		Promotion shopPromotion = new Promotion();
		shopPromotion.setShop(shop);
		shopPromotion.setDiscountPercent(new BigDecimal("25"));
		shopPromotion.getProducts().add(activeProduct);
		globalPromotion.setDiscountPercent(new BigDecimal("20"));
		stubActivePromotions(List.of(shopPromotion), List.of(globalPromotion));

		assertEquals(0, new BigDecimal("750.00").compareTo(promotionService.getEffectivePrice(7L)));
	}

	@Test
	void appliesShopPromotionWhenNoPlatformPromotionApplies() {
		Shop shop = new Shop();
		shop.setId(3L);
		activeProduct.setShop(shop);

		Promotion shopPromotion = new Promotion();
		shopPromotion.setShop(shop);
		shopPromotion.setDiscountPercent(new BigDecimal("15"));
		shopPromotion.getProducts().add(activeProduct);
		stubActivePromotions(List.of(shopPromotion), List.of());

		assertEquals(0, new BigDecimal("850.00").compareTo(promotionService.getEffectivePrice(7L)));
	}

	private void stubActivePromotions(
			List<Promotion> shopPromotions, List<Promotion> platformPromotions) {
		when(productRepository.findAllForPromotionPricing(List.of(7L))).thenReturn(List.of(activeProduct));
		when(vendorPromotionRepository.findActiveShopPromotionsForProducts(
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(List.of(7L))))
				.thenReturn(shopPromotions);
		when(platformPromotionRepository.findActivePlatformPromotions(
				org.mockito.ArgumentMatchers.eq(ActiveStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(PromotionType.PRODUCT_DISCOUNT),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(platformPromotions);
	}
}
