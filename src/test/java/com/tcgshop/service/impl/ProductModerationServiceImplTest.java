package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.ProductBanRequest;
import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.ProductModerationMapper;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductModerationServiceImplTest {

	@Mock private ProductRepository productRepository;
	@Mock private com.tcgshop.repository.ProductImageRepository productImageRepository;
	@Mock private ShopRepository shopRepository;
	@Mock private ProductModerationMapper productModerationMapper;
	@Mock private NotificationService notificationService;
	@Mock private CurrentUser currentUser;
	@InjectMocks private ProductModerationServiceImpl service;

	@Test
	void searchLoadsMainImagesOnceForThePage() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		Product product = product(ProductStatus.ACTIVE);
		var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		when(productRepository.searchForModeration(null, null, null, null, pageable))
				.thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(product)));
		var image = org.mockito.Mockito.mock(com.tcgshop.repository.ProductThumbnailProjection.class);
		when(image.getProductId()).thenReturn(product.getId());
		when(image.getImageUrl()).thenReturn("https://example.com/main.png");
		when(productImageRepository.findThumbnails(java.util.List.of(product.getId()))).thenReturn(java.util.List.of(image));
		when(productModerationMapper.toResponse(product, image.getImageUrl())).thenReturn(response(ProductStatus.ACTIVE, null));
		assertThat(service.search(null, null, null, null, pageable).content()).hasSize(1);
		verify(productImageRepository).findThumbnails(java.util.List.of(product.getId()));
		verify(productModerationMapper).toResponse(product, image.getImageUrl());
	}

	@Test
	void banStoresReasonAndNotifiesShopOwner() {
		Product product = product(ProductStatus.ACTIVE);
		ProductModerationResponse response = response(ProductStatus.BANNED, "Nội dung vi phạm");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(productRepository.findNotDeletedByIdWithShopOwner(15L)).thenReturn(Optional.of(product));
		when(productRepository.save(product)).thenReturn(product);
		when(productModerationMapper.toResponse(product)).thenReturn(response);
		ProductBanRequest request = new ProductBanRequest();
		request.setReason("  Nội dung vi phạm  ");

		ProductModerationResponse result = service.ban(15L, request);

		assertThat(result.status()).isEqualTo(ProductStatus.BANNED);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.BANNED);
		assertThat(product.getBanReason()).isEqualTo("Nội dung vi phạm");
		verify(notificationService).send(25L, NotificationType.SYSTEM,
				"Sản phẩm đã bị gỡ khỏi hệ thống",
				"Sản phẩm \"Thẻ mẫu\" đã bị gỡ. Lý do: Nội dung vi phạm", "/profile");
	}

	@Test
	void banRequiresReasonAndDoesNotChangeProduct() {
		Product product = product(ProductStatus.ACTIVE);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(productRepository.findNotDeletedByIdWithShopOwner(15L)).thenReturn(Optional.of(product));
		ProductBanRequest request = new ProductBanRequest();
		request.setReason(" ");

		assertThatThrownBy(() -> service.ban(15L, request))
				.isInstanceOf(BadRequestException.class);

		assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
		verify(productRepository, never()).save(any());
		verify(notificationService, never()).send(any(), any(), any(), any(), any());
	}

	@Test
	void restoreClearsReasonAndNotifiesOwner() {
		Product product = product(ProductStatus.BANNED);
		product.setBanReason("Lý do cũ");
		ProductModerationResponse response = response(ProductStatus.ACTIVE, null);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(productRepository.findNotDeletedByIdWithShopOwner(15L)).thenReturn(Optional.of(product));
		when(productRepository.save(product)).thenReturn(product);
		when(productModerationMapper.toResponse(product)).thenReturn(response);

		ProductModerationResponse result = service.restore(15L);

		assertThat(result.status()).isEqualTo(ProductStatus.ACTIVE);
		assertThat(product.getBanReason()).isNull();
		verify(notificationService).send(eq(25L), eq(NotificationType.SYSTEM),
				eq("Sản phẩm đã được khôi phục"),
				eq("Sản phẩm \"Thẻ mẫu\" đã được khôi phục và chuyển sang trạng thái đang bán."),
				eq("/profile"));
	}

	@Test
	void restoreRejectsProductThatIsNotBanned() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(productRepository.findNotDeletedByIdWithShopOwner(15L))
				.thenReturn(Optional.of(product(ProductStatus.INACTIVE)));

		assertThatThrownBy(() -> service.restore(15L))
				.isInstanceOf(ConflictException.class);
		verify(productRepository, never()).save(any());
	}

	@Test
	void vendorCannotUseModerationService() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> service.getById(15L))
				.isInstanceOf(ForbiddenException.class);
		verify(productRepository, never()).findNotDeletedByIdWithShopOwner(any());
	}

	private Product product(ProductStatus status) {
		User owner = new User();
		owner.setId(25L);
		Shop shop = new Shop();
		shop.setId(5L);
		shop.setName("Cửa hàng mẫu");
		shop.setOwner(owner);
		Product product = new Product();
		product.setId(15L);
		product.setName("Thẻ mẫu");
		product.setShop(shop);
		product.setType(ProductType.SINGLE);
		product.setStatus(status);
		return product;
	}

	private ProductModerationResponse response(ProductStatus status, String reason) {
		return new ProductModerationResponse(15L, "Thẻ mẫu", 5L, "Cửa hàng mẫu",
				ProductType.SINGLE, status, 3, reason, null);
	}
}
