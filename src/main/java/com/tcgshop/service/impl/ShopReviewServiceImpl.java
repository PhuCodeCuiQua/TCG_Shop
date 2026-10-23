package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ShopLockRequest;
import com.tcgshop.dto.request.ShopRejectRequest;
import com.tcgshop.dto.response.PageResponse;
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
import com.tcgshop.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ShopReviewServiceImpl implements ShopReviewService {

	private final ShopRepository shopRepository;
	private final RoleRepository roleRepository;
	private final ShopReviewMapper shopReviewMapper;
	private final NotificationService notificationService;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ShopReviewResponse> search(String keyword, ShopStatus status, Pageable pageable) {
		ensureManagerOrAdmin();
		return PageResponse.from(
				shopRepository.searchForReview(status, normalizeKeyword(keyword), pageable),
				shopReviewMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public ShopReviewResponse getById(Long shopId) {
		ensureManagerOrAdmin();
		return shopReviewMapper.toResponse(findShop(shopId));
	}

	@Override
	@Transactional
	public ShopReviewResponse approve(Long shopId) {
		ensureManagerOrAdmin();
		Shop shop = findShop(shopId);
		requireStatus(shop, ShopStatus.PENDING, "Chỉ có thể duyệt hồ sơ đang chờ xử lý");

		User owner = shop.getOwner();
		if (owner == null) {
			throw new ConflictException("Hồ sơ shop chưa có chủ sở hữu hợp lệ");
		}
		Role vendorRole = roleRepository.findByName(RoleName.ROLE_VENDOR)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò Nhà bán hàng"));

		// Thêm role Vendor vào tập hiện tại, không thay thế ROLE_USER hoặc các role hiện có.
		owner.getRoles().add(vendorRole);
		shop.setStatus(ShopStatus.APPROVED);
		shop.setRejectReason(null);
		Shop savedShop = shopRepository.save(shop);
		notifyOwner(savedShop, "Cửa hàng đã được duyệt",
				"Hồ sơ cửa hàng \"" + savedShop.getName() + "\" đã được duyệt. Bạn có thể bắt đầu bán hàng.",
				"/shops/" + savedShop.getSlug());
		return shopReviewMapper.toResponse(savedShop);
	}

	@Override
	@Transactional
	public ShopReviewResponse reject(Long shopId, ShopRejectRequest request) {
		ensureManagerOrAdmin();
		Shop shop = findShop(shopId);
		requireStatus(shop, ShopStatus.PENDING, "Chỉ có thể từ chối hồ sơ đang chờ xử lý");
		if (request == null || request.getReason() == null || request.getReason().isBlank()) {
			throw new BadRequestException("Vui lòng nhập lý do từ chối");
		}
		String reason = request.getReason().trim();
		if (reason.length() > 255) {
			throw new BadRequestException("Lý do từ chối tối đa 255 ký tự");
		}

		shop.setStatus(ShopStatus.REJECTED);
		shop.setRejectReason(reason);
		Shop savedShop = shopRepository.save(shop);
		notifyOwner(savedShop, "Hồ sơ cửa hàng chưa được duyệt",
				"Hồ sơ cửa hàng \"" + savedShop.getName() + "\" chưa được duyệt. Lý do: " + reason,
				"/seller-registration");
		return shopReviewMapper.toResponse(savedShop);
	}

	@Override
	@Transactional
	public ShopReviewResponse suspend(Long shopId, ShopLockRequest request) {
		ensureManagerOrAdmin();
		Shop shop = findShop(shopId);
		requireStatus(shop, ShopStatus.APPROVED, "Chỉ có thể khóa cửa hàng đang được duyệt");

		shop.setStatus(ShopStatus.SUSPENDED);
		Shop savedShop = shopRepository.save(shop);
		String reason = request == null || request.getReason() == null || request.getReason().isBlank()
				? ""
				: " Lý do: " + request.getReason().trim();
		notifyOwner(savedShop, "Cửa hàng đã tạm ngừng hoạt động",
				"Cửa hàng \"" + savedShop.getName() + "\" đã bị tạm ngừng." + reason,
				"/seller-registration");
		return shopReviewMapper.toResponse(savedShop);
	}

	@Override
	@Transactional
	public ShopReviewResponse reactivate(Long shopId) {
		ensureManagerOrAdmin();
		Shop shop = findShop(shopId);
		requireStatus(shop, ShopStatus.SUSPENDED, "Chỉ có thể mở lại cửa hàng đang bị tạm ngừng");

		shop.setStatus(ShopStatus.APPROVED);
		shop.setRejectReason(null);
		Shop savedShop = shopRepository.save(shop);
		notifyOwner(savedShop, "Cửa hàng đã hoạt động trở lại",
				"Cửa hàng \"" + savedShop.getName() + "\" đã được mở lại.",
				"/shops/" + savedShop.getSlug());
		return shopReviewMapper.toResponse(savedShop);
	}

	private Shop findShop(Long shopId) {
		return shopRepository.findNotDeletedByIdWithOwnerRoles(shopId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng"));
	}

	private void requireStatus(Shop shop, ShopStatus requiredStatus, String message) {
		if (shop.getStatus() != requiredStatus) {
			throw new ConflictException(message);
		}
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER)
				&& !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền quản lý hồ sơ cửa hàng");
		}
	}

	private void notifyOwner(Shop shop, String title, String content, String link) {
		if (shop.getOwner() != null) {
			notificationService.send(
					shop.getOwner().getId(), NotificationType.SHOP_APPROVAL, title, content, link);
		}
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}
