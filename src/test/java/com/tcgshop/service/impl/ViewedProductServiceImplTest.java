package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.ViewedProduct;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.ViewedProductRepository;
import com.tcgshop.security.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ViewedProductServiceImplTest {
	private static final Long USER_ID = 15L;
	private static final Long PRODUCT_ID = 42L;

	@Mock private ViewedProductRepository viewedProductRepository;
	@Mock private ProductRepository productRepository;
	@Mock private UserRepository userRepository;
	@Mock private CurrentUser currentUser;
	@InjectMocks private ViewedProductServiceImpl service;

	@Test
	void recordUpdatesExistingTimestampWithoutCreatingDuplicate() {
		Product product = publicProduct();
		ViewedProduct existing = new ViewedProduct();
		existing.setId(1L);
		java.time.LocalDateTime oldViewedAt = java.time.LocalDateTime.now().minusDays(1);
		existing.setViewedAt(oldViewedAt);
		allowCurrentUser();
		when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(new User()));
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
		when(viewedProductRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(existing));
		when(viewedProductRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(existing), PageRequest.of(0, 20), 1));

		service.record(USER_ID, PRODUCT_ID);

		assertThat(existing.getViewedAt()).isAfter(oldViewedAt);
		verify(viewedProductRepository).saveAndFlush(existing);
		verify(viewedProductRepository, never()).deleteByUserIdKeepingIds(any(), any());
	}

	@Test
	void recordTrimsEveryEntryAfterTheTwentyMostRecent() {
		Product product = publicProduct();
		User user = new User();
		user.setId(USER_ID);
		allowCurrentUser();
		when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
		when(viewedProductRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
		List<ViewedProduct> latest = new ArrayList<>();
		for (long id = 1; id <= 20; id++) {
			ViewedProduct viewed = new ViewedProduct();
			viewed.setId(id);
			latest.add(viewed);
		}
		when(viewedProductRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(latest, PageRequest.of(0, 20), 21));

		service.record(USER_ID, PRODUCT_ID);

		verify(viewedProductRepository).deleteByUserIdKeepingIds(USER_ID, latest.stream().map(ViewedProduct::getId).toList());
	}

	@Test
	void recordIgnoresProductThatIsNoLongerPublic() {
		Product hidden = publicProduct();
		hidden.setStatus(ProductStatus.INACTIVE);
		allowCurrentUser();
		when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(new User()));
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(hidden));

		service.record(USER_ID, PRODUCT_ID);

		verify(viewedProductRepository, never()).saveAndFlush(any(ViewedProduct.class));
		verify(viewedProductRepository, never()).findByUserIdAndProductId(USER_ID, PRODUCT_ID);
	}

	@Test
	void recordRejectsAnotherUsersHistory() {
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(USER_ID + 1);

		assertThatThrownBy(() -> service.record(USER_ID, PRODUCT_ID))
				.isInstanceOf(ForbiddenException.class);
		verify(productRepository, never()).findById(PRODUCT_ID);
	}

	private void allowCurrentUser() {
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
		when(currentUser.getId()).thenReturn(USER_ID);
	}

	private Product publicProduct() {
		Shop shop = new Shop();
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		Product product = new Product();
		product.setId(PRODUCT_ID);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product.setShop(shop);
		return product;
	}
}
