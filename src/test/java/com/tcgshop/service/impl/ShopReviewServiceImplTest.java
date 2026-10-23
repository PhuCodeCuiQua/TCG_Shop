package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.ShopLockRequest;
import com.tcgshop.dto.request.ShopRejectRequest;
import com.tcgshop.dto.response.ShopReviewResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ShopReviewMapper;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.NotificationService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ShopReviewServiceImplTest {

	@Mock
	private ShopRepository shopRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private ShopReviewMapper shopReviewMapper;
	@Mock
	private NotificationService notificationService;
	@Mock
	private CurrentUser currentUser;
	@InjectMocks
	private ShopReviewServiceImpl service;

	@Test
	void searchNormalizesKeywordAndMapsPage() {
		PageRequest pageable = PageRequest.of(0, 20);
		Shop shop = shop(30L, ShopStatus.PENDING);
		ShopReviewResponse response = response(shop, ShopStatus.PENDING);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.searchForReview(ShopStatus.PENDING, "card shop", pageable))
				.thenReturn(new PageImpl<>(List.of(shop), pageable, 1));
		when(shopReviewMapper.toResponse(shop)).thenReturn(response);

		var result = service.search(" card shop ", ShopStatus.PENDING, pageable);

		assertThat(result.content()).containsExactly(response);
		assertThat(result.totalElements()).isEqualTo(1);
		verify(shopRepository).searchForReview(ShopStatus.PENDING, "card shop", pageable);
	}

	@Test
	void managerCanApprovePendingShopAndPreserveUserRole() {
		Shop shop = shop(30L, ShopStatus.PENDING);
		Role vendorRole = role(RoleName.ROLE_VENDOR);
		ShopReviewResponse response = response(shop, ShopStatus.APPROVED);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));
		when(roleRepository.findByName(RoleName.ROLE_VENDOR)).thenReturn(Optional.of(vendorRole));
		when(shopRepository.save(shop)).thenReturn(shop);
		when(shopReviewMapper.toResponse(shop)).thenReturn(response);

		ShopReviewResponse result = service.approve(30L);

		assertThat(result).isEqualTo(response);
		assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
		assertThat(shop.getRejectReason()).isNull();
		assertThat(shop.getOwner().getRoles())
				.extracting(Role::getName)
				.contains(RoleName.ROLE_USER, RoleName.ROLE_VENDOR);
		verify(notificationService).send(
				2L, NotificationType.SHOP_APPROVAL, "Cửa hàng đã được duyệt",
				"Hồ sơ cửa hàng \"Card Shop\" đã được duyệt. Bạn có thể bắt đầu bán hàng.",
				"/profile");
	}

	@Test
	void rejectStoresTrimmedReasonAndNotifiesOwner() {
		Shop shop = shop(30L, ShopStatus.PENDING);
		ShopReviewResponse response = response(shop, ShopStatus.REJECTED);
		ShopRejectRequest request = new ShopRejectRequest();
		request.setReason("  Thiếu giấy tờ hợp lệ  ");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));
		when(shopRepository.save(shop)).thenReturn(shop);
		when(shopReviewMapper.toResponse(shop)).thenReturn(response);

		ShopReviewResponse result = service.reject(30L, request);

		assertThat(result).isEqualTo(response);
		assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
		assertThat(shop.getRejectReason()).isEqualTo("Thiếu giấy tờ hợp lệ");
		verify(notificationService).send(
				eq(2L), eq(NotificationType.SHOP_APPROVAL), eq("Hồ sơ cửa hàng chưa được duyệt"),
				eq("Hồ sơ cửa hàng \"Card Shop\" chưa được duyệt. Lý do: Thiếu giấy tờ hợp lệ"),
				eq("/profile"));
	}

	@Test
	void rejectRequiresReason() {
		Shop shop = shop(30L, ShopStatus.PENDING);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));
		ShopRejectRequest request = new ShopRejectRequest();

		assertThatThrownBy(() -> service.reject(30L, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Vui lòng nhập lý do từ chối");

		verify(shopRepository, never()).save(any(Shop.class));
		verify(notificationService, never()).send(any(), any(), any(), any(), any());
	}

	@Test
	void onlyPendingShopCanBeApproved() {
		Shop shop = shop(30L, ShopStatus.REJECTED);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));

		assertThatThrownBy(() -> service.approve(30L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Chỉ có thể duyệt hồ sơ đang chờ xử lý");

		verify(roleRepository, never()).findByName(any());
		verify(shopRepository, never()).save(any(Shop.class));
	}

	@Test
	void managerCanSuspendApprovedShopWithoutChangingProductRows() {
		Shop shop = shop(30L, ShopStatus.APPROVED);
		ShopLockRequest request = new ShopLockRequest();
		request.setReason("  Hồ sơ cần xác minh  ");
		ShopReviewResponse response = response(shop, ShopStatus.SUSPENDED);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));
		when(shopRepository.save(shop)).thenReturn(shop);
		when(shopReviewMapper.toResponse(shop)).thenReturn(response);

		var result = service.suspend(30L, request);

		assertThat(result).isEqualTo(response);
		assertThat(shop.getStatus()).isEqualTo(ShopStatus.SUSPENDED);
		verify(notificationService).send(
				2L, NotificationType.SHOP_APPROVAL, "Cửa hàng đã tạm ngừng hoạt động",
				"Cửa hàng \"Card Shop\" đã bị tạm ngừng. Lý do: Hồ sơ cần xác minh",
				"/profile");
	}

	@Test
	void reactivateSuspendedShopRestoresApprovedStatus() {
		Shop shop = shop(30L, ShopStatus.SUSPENDED);
		shop.setRejectReason("Old rejection");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.of(shop));
		when(shopRepository.save(shop)).thenReturn(shop);
		when(shopReviewMapper.toResponse(shop)).thenReturn(response(shop, ShopStatus.APPROVED));

		service.reactivate(30L);

		assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
		assertThat(shop.getRejectReason()).isNull();
	}

	@Test
	void nonManagerCannotModerateShop() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> service.search(null, null, PageRequest.of(0, 20)))
				.isInstanceOf(ForbiddenException.class);

		verify(shopRepository, never()).searchForReview(any(), any(), any());
	}

	@Test
	void missingShopReturnsNotFound() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(shopRepository.findNotDeletedByIdWithOwnerRoles(30L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(30L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Shop shop(Long id, ShopStatus status) {
		Role userRole = role(RoleName.ROLE_USER);
		User owner = new User();
		owner.setId(2L);
		owner.setEmail("owner@example.com");
		owner.setFullName("Shop Owner");
		owner.setRoles(new HashSet<>(List.of(userRole)));

		Shop shop = new Shop();
		shop.setId(id);
		shop.setName("Card Shop");
		shop.setSlug("card-shop");
		shop.setOwner(owner);
		shop.setStatus(status);
		shop.setDeleted(false);
		return shop;
	}

	private Role role(RoleName name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}

	private ShopReviewResponse response(Shop shop, ShopStatus status) {
		return new ShopReviewResponse(
				shop.getId(), shop.getName(), shop.getSlug(), null, null, null, null, null,
				status, shop.getRejectReason(), 2L, "owner@example.com", "Shop Owner", null);
	}
}
