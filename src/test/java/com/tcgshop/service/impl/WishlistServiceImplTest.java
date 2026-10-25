package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.WishlistProductResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.Wishlist;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.WishlistMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.WishlistProductRepository;
import com.tcgshop.repository.WishlistProductProjection;
import com.tcgshop.repository.WishlistRepository;
import com.tcgshop.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {
	private static final Long USER_ID = 8L;
	private static final Long PRODUCT_ID = 31L;

	@Mock private WishlistRepository wishlistRepository;
	@Mock private WishlistProductRepository productRepository;
	@Mock private UserRepository userRepository;
	@Mock private WishlistMapper wishlistMapper;
	@Mock private CurrentUser currentUser;
	@InjectMocks private WishlistServiceImpl service;

	@Test
	void toggleAddsOnceAndSynchronizesFavoriteCount() {
		Product product = availableProduct();
		User user = new User();
		user.setId(USER_ID);
		when(currentUser.getId()).thenReturn(USER_ID);
		when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
		when(wishlistRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
		when(wishlistRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);

		var response = service.toggle(PRODUCT_ID);

		assertThat(response.wishlisted()).isTrue();
		assertThat(response.favoriteCount()).isEqualTo(1);
		assertThat(product.getFavoriteCount()).isEqualTo(1);
		verify(wishlistRepository).save(any(Wishlist.class));
		verify(productRepository).saveAndFlush(product);
	}

	@Test
	void toggleRemovesExistingWishlistAndRecounts() {
		Product product = availableProduct();
		Wishlist existing = new Wishlist();
		when(currentUser.getId()).thenReturn(USER_ID);
		when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
		when(wishlistRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(existing));
		when(wishlistRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);

		var response = service.toggle(PRODUCT_ID);

		assertThat(response.wishlisted()).isFalse();
		assertThat(response.favoriteCount()).isZero();
		verify(wishlistRepository).delete(existing);
		verify(wishlistRepository, never()).save(any(Wishlist.class));
	}

	@Test
	void toggleDoesNotExposeUnapprovedShopProducts() {
		Product hidden = availableProduct();
		hidden.getShop().setStatus(ShopStatus.PENDING);
		when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(hidden));

		assertThatThrownBy(() -> service.toggle(PRODUCT_ID))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(wishlistRepository, never()).save(any(Wishlist.class));
	}

	@Test
	void statusesAreReturnedForRequestedIds() {
		when(currentUser.getId()).thenReturn(USER_ID);
		when(wishlistRepository.findProductIdsByUserIdAndProductIdIn(USER_ID, List.of(2L, 3L)))
				.thenReturn(List.of(3L));

		assertThat(service.getStatuses(List.of(2L, 3L)))
				.containsEntry(2L, false)
				.containsEntry(3L, true);
	}

	@Test
	void wishlistPageMapsPublicProjectionToResponse() {
		var projection = org.mockito.Mockito.mock(WishlistProductProjection.class);
		var response = new WishlistProductResponse(1L, PRODUCT_ID, "Card", "Shop", null, null, 4, 3);
		when(currentUser.getId()).thenReturn(USER_ID);
		when(wishlistRepository.findVisibleProductsByUserId(USER_ID, PageRequest.of(0, 12)))
				.thenReturn(new PageImpl<>(List.of(projection)));
		when(wishlistMapper.toResponse(projection)).thenReturn(response);

		assertThat(service.getCurrentUserWishlist(PageRequest.of(0, 12)).getContent())
				.containsExactly(response);
	}

	private Product availableProduct() {
		Shop shop = new Shop();
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		Product product = new Product();
		product.setId(PRODUCT_ID);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product.setFavoriteCount(0);
		product.setShop(shop);
		return product;
	}
}
