package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.VendorProductEditRequest;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.VendorProductEditMapper;
import com.tcgshop.repository.VendorProductManageRepository;
import com.tcgshop.repository.VendorProductOrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorProductOwnershipTest {
	private static final Long VENDOR_USER_ID = 10L;
	private static final Long OTHER_VENDOR_USER_ID = 20L;
	private static final Long PRODUCT_ID = 300L;

	@Mock private VendorProductManageRepository productRepository;
	@Mock private VendorProductOrderRepository orderRepository;
	@Mock private VendorProductEditMapper mapper;
	@Mock private CurrentUser currentUser;
	@Mock private CloudinaryService cloudinaryService;
	@InjectMocks private VendorProductManageServiceImpl service;

	private Product otherVendorsProduct;

	@BeforeEach
	void setUp() {
		User owner = new User();
		owner.setId(OTHER_VENDOR_USER_ID);
		Shop shop = new Shop();
		shop.setId(200L);
		shop.setOwner(owner);
		otherVendorsProduct = new Product();
		otherVendorsProduct.setId(PRODUCT_ID);
		otherVendorsProduct.setShop(shop);

		when(currentUser.hasRole(RoleName.ROLE_VENDOR)).thenReturn(true);
		when(currentUser.getId()).thenReturn(VENDOR_USER_ID);
		when(productRepository.findNotDeletedWithOwnerAndImages(PRODUCT_ID))
				.thenReturn(Optional.of(otherVendorsProduct));
	}

	@Test
	void editingProductOwnedByAnotherVendorIsForbidden() {
		assertThatThrownBy(() -> service.updateOwnedProduct(PRODUCT_ID, new VendorProductEditRequest()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void deletingProductOwnedByAnotherVendorIsForbidden() {
		assertThatThrownBy(() -> service.softDeleteOwnedProduct(PRODUCT_ID))
				.isInstanceOf(ForbiddenException.class);
		verify(orderRepository, org.mockito.Mockito.never())
				.countForProductInStatuses(org.mockito.ArgumentMatchers.eq(PRODUCT_ID), org.mockito.ArgumentMatchers.anyList());
	}
}
