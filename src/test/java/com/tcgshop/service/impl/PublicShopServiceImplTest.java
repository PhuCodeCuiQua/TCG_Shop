package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.PublicShopProductResponse;
import com.tcgshop.dto.response.PublicShopResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.mapper.PublicShopMapper;
import com.tcgshop.mapper.PublicShopProductMapper;
import com.tcgshop.repository.PublicShopProductProjection;
import com.tcgshop.repository.PublicShopProductRepository;
import com.tcgshop.repository.ReviewAggregateRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.PromotionService;
import com.tcgshop.service.SeriesCatalogService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PublicShopServiceImplTest {
	private ShopRepository shopRepository;
	private PublicShopProductRepository productRepository;
	private PublicShopMapper shopMapper;
	private PublicShopProductMapper productMapper;
	private ReviewAggregateRepository reviewAggregateRepository;
	private PromotionService promotionService;
	private GameCatalogService gameCatalogService;
	private SeriesCatalogService seriesCatalogService;
	private PublicShopServiceImpl service;

	@BeforeEach
	void setUp() {
		shopRepository = mock(ShopRepository.class);
		productRepository = mock(PublicShopProductRepository.class);
		shopMapper = mock(PublicShopMapper.class);
		productMapper = mock(PublicShopProductMapper.class);
		reviewAggregateRepository = mock(ReviewAggregateRepository.class);
		promotionService = mock(PromotionService.class);
		gameCatalogService = mock(GameCatalogService.class);
		seriesCatalogService = mock(SeriesCatalogService.class);
		service = new PublicShopServiceImpl(shopRepository, productRepository, shopMapper, productMapper,
				reviewAggregateRepository, promotionService, gameCatalogService, seriesCatalogService);
	}

	@Test
	void trimsSearchKeywordAndAppliesEffectivePricesInBulk() {
		Shop shop = new Shop();
		shop.setId(3L);
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		when(shopRepository.findById(3L)).thenReturn(Optional.of(shop));
		when(shopMapper.toResponse(shop)).thenReturn(
				new PublicShopResponse(3L, "Test shop", "test-shop", null, null, null, null, null, null, 0));
		when(reviewAggregateRepository.summarizeByShopId(3L)).thenReturn(Optional.empty());

		PublicShopProductProjection projection = mock(PublicShopProductProjection.class);
		when(projection.getId()).thenReturn(19L);
		var pageable = PageRequest.of(0, 12);
		when(productRepository.findPublicProducts(eq(3L), eq(ShopStatus.APPROVED), eq(ProductStatus.ACTIVE),
				eq(ProductType.SUPPLY), eq("Sogeking"), any(), any(), any(), any(), any(),
				eq(ProductType.SINGLE), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(projection)));
		when(productMapper.toResponse(projection)).thenReturn(new PublicShopProductResponse(
				19L, "Sogeking", null, new BigDecimal("100000"), 2, ProductType.SUPPLY, null));
		when(promotionService.getEffectivePrices(List.of(19L))).thenReturn(Map.of(19L, new BigDecimal("80000")));

		var minPrice = new BigDecimal("90000");
		var maxPrice = new BigDecimal("200000");
		var response = service.getApprovedShopPage(3L, ProductType.SUPPLY, "  Sogeking  ",
				8L, 15L, Rarity.RARE, minPrice, maxPrice, pageable);

		assertThat(response.products().getContent()).singleElement().satisfies(product -> {
			assertThat(product.originalPrice()).isEqualByComparingTo("100000");
			assertThat(product.effectivePrice()).isEqualByComparingTo("80000");
		});
		verify(productRepository).findPublicProducts(3L, ShopStatus.APPROVED, ProductStatus.ACTIVE,
				ProductType.SUPPLY, "Sogeking", 8L, 15L, Rarity.RARE, minPrice, maxPrice,
				ProductType.SINGLE, pageable);
		verify(promotionService).getEffectivePrices(List.of(19L));
	}
}
