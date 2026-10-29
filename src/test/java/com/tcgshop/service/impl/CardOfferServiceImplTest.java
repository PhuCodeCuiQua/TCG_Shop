package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.repository.CardOfferRepository;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardOfferServiceImplTest {
	@Mock private CardOfferRepository repository;
	@Mock private PromotionService promotionService;
	@InjectMocks private CardOfferServiceImpl service;

	@Test
	void sellerRatingComesFromShopAndUnratedShopsSortLast() {
		Product rated = offer(1L, new BigDecimal("4.25"), 8);
		rated.setAvgRating(new BigDecimal("1.0"));
		Product unrated = offer(2L, BigDecimal.ZERO, 0);
		Product missingAverage = offer(3L, null, 2);
		when(repository.findAvailableByCardId(7L)).thenReturn(List.of(unrated, rated, missingAverage));
		when(promotionService.getEffectivePrices(anyCollection())).thenReturn(Map.of());

		var offers = service.getOffers(7L, null, "rating", "desc").offers();
		assertThat(offers.getFirst().productId()).isEqualTo(1L);
		assertThat(offers.getFirst().shopRating()).isEqualByComparingTo("4.25");
		assertThat(offers.getFirst().shopRatingCount()).isEqualTo(8);
		assertThat(offers.subList(1, 3)).allSatisfy(offer -> assertThat(offer.shopRating()).isNull());
	}

	private Product offer(Long id, BigDecimal average, int count) {
		Shop shop = new Shop();
		shop.setId(id);
		shop.setName("Shop " + id);
		shop.setAvgRating(average);
		shop.setRatingCount(count);
		Product product = new Product();
		product.setId(id);
		product.setShop(shop);
		product.setPrice(new BigDecimal("10000"));
		product.setStock(2);
		return product;
	}
}
