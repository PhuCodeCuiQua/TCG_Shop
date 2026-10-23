package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ProductBanRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ProductModerationResponse;
import com.tcgshop.dto.response.ShopOptionResponse;
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
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ProductModerationMapper;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.NotificationService;
import com.tcgshop.service.ProductModerationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ProductModerationServiceImpl implements ProductModerationService {

	private final ProductRepository productRepository;
	private final ShopRepository shopRepository;
	private final ProductModerationMapper productModerationMapper;
	private final NotificationService notificationService;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductModerationResponse> search(
			Long shopId, ProductType type, ProductStatus status, String keyword, Pageable pageable) {
		ensureManagerOrAdmin();
		return PageResponse.from(
				productRepository.searchForModeration(shopId, type, status, normalizeKeyword(keyword), pageable),
				productModerationMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShopOptionResponse> getShopOptions() {
		ensureManagerOrAdmin();
		return shopRepository.findAllNotDeletedForOptions().stream()
				.map(productModerationMapper::toShopOption)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductModerationResponse getById(Long productId) {
		ensureManagerOrAdmin();
		return productModerationMapper.toResponse(findProduct(productId));
	}

	@Override
	@Transactional
	public ProductModerationResponse ban(Long productId, ProductBanRequest request) {
		ensureManagerOrAdmin();
		Product product = findProduct(productId);
		if (product.getStatus() == ProductStatus.BANNED) {
			throw new ConflictException("Sản phẩm đã bị gỡ trước đó");
		}
		if (request == null || request.getReason() == null || request.getReason().isBlank()) {
			throw new BadRequestException("Vui lòng nhập lý do gỡ sản phẩm");
		}
		String reason = request.getReason().trim();
		if (reason.length() > 255) {
			throw new BadRequestException("Lý do gỡ sản phẩm tối đa 255 ký tự");
		}

		product.setStatus(ProductStatus.BANNED);
		product.setBanReason(reason);
		Product savedProduct = productRepository.save(product);
		notifyVendor(savedProduct, "Sản phẩm đã bị gỡ khỏi hệ thống",
				"Sản phẩm \"" + savedProduct.getName() + "\" đã bị gỡ. Lý do: " + reason);
		return productModerationMapper.toResponse(savedProduct);
	}

	@Override
	@Transactional
	public ProductModerationResponse restore(Long productId) {
		ensureManagerOrAdmin();
		Product product = findProduct(productId);
		if (product.getStatus() != ProductStatus.BANNED) {
			throw new ConflictException("Chỉ có thể khôi phục sản phẩm đã bị gỡ");
		}

		product.setStatus(ProductStatus.ACTIVE);
		product.setBanReason(null);
		Product savedProduct = productRepository.save(product);
		notifyVendor(savedProduct, "Sản phẩm đã được khôi phục",
				"Sản phẩm \"" + savedProduct.getName() + "\" đã được khôi phục và chuyển sang trạng thái đang bán.");
		return productModerationMapper.toResponse(savedProduct);
	}

	private Product findProduct(Long productId) {
		return productRepository.findNotDeletedByIdWithShopOwner(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
	}

	private void notifyVendor(Product product, String title, String content) {
		Shop shop = product.getShop();
		User owner = shop == null ? null : shop.getOwner();
		if (owner == null) {
			throw new ConflictException("Không thể gửi thông báo vì sản phẩm chưa có chủ cửa hàng hợp lệ");
		}
		notificationService.send(owner.getId(), NotificationType.SYSTEM, title, content, "/profile");
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER)
				&& !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền quản lý sản phẩm");
		}
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}
