package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorProductEditRequest;
import com.tcgshop.dto.response.VendorProductActionResponse;
import com.tcgshop.dto.response.VendorProductEditResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorProductEditMapper;
import com.tcgshop.repository.VendorProductManageRepository;
import com.tcgshop.repository.VendorProductOrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.VendorProductManageService;
import com.tcgshop.service.UploadResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorProductManageServiceImpl implements VendorProductManageService {
	private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
			OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
			OrderStatus.DELIVERY_FAILED, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED);
	private static final String OPTIMISTIC_LOCK_MESSAGE = "Sản phẩm vừa được cập nhật bởi người khác, vui lòng thử lại";

	private final VendorProductManageRepository productRepository;
	private final VendorProductOrderRepository orderRepository;
	private final VendorProductEditMapper mapper;
	private final CurrentUser currentUser;
	private final CloudinaryService cloudinaryService;

	@Override
	@Transactional(readOnly = true)
	public VendorProductEditResponse getOwnedProduct(Long productId) {
		return mapper.toResponse(findOwnedProduct(productId));
	}

	@Override
	@Transactional
	public void updateOwnedProduct(Long productId, VendorProductEditRequest request) {
		Product product = findOwnedProduct(productId);
		String uploadedPublicId = null;
		String oldPublicId = primaryImage(product).map(ProductImage::getImagePublicId).orElse(null);
		try {
			if (request.getImage() != null && !request.getImage().isEmpty()) {
				UploadResult upload = cloudinaryService.uploadImage(request.getImage(), "tcgshop/products");
				uploadedPublicId = upload.publicId();
				ProductImage image = primaryImage(product).orElseGet(() -> {
					ProductImage created = new ProductImage();
					created.setProduct(product);
					created.setIsMain(true);
					created.setDisplayOrder(0);
					product.getImages().add(created);
					return created;
				});
				image.setImageUrl(upload.url());
				image.setImagePublicId(upload.publicId());
			}
			product.setPrice(request.getPrice());
			product.setStock(request.getStock());
			product.setDescription(request.getDescription());
			productRepository.saveAndFlush(product);
		} catch (OptimisticLockingFailureException exception) {
			deleteAfterRollback(uploadedPublicId);
			throw new BadRequestException(OPTIMISTIC_LOCK_MESSAGE);
		} catch (RuntimeException exception) {
			deleteAfterRollback(uploadedPublicId);
			throw exception;
		}
		if (uploadedPublicId != null && oldPublicId != null && !oldPublicId.equals(uploadedPublicId)) {
			deleteAfterCommit(oldPublicId);
		}
	}

	@Override
	@Transactional
	public VendorProductActionResponse toggleOwnedProduct(Long productId) {
		Product product = findOwnedProduct(productId);
		if (product.getStatus() == ProductStatus.BANNED) {
			throw new BadRequestException("Sản phẩm bị khóa không thể bật hoặc tắt bằng thao tác này");
		}
		if (product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.INACTIVE) {
			throw new BadRequestException("Chỉ có thể bật hoặc ẩn sản phẩm đang bán hoặc đang ẩn");
		}
		product.setStatus(product.getStatus() == ProductStatus.ACTIVE ? ProductStatus.INACTIVE : ProductStatus.ACTIVE);
		flushOrConflict(product);
		return new VendorProductActionResponse(product.getId(), product.getStatus(), "Đã cập nhật trạng thái sản phẩm");
	}

	@Override
	@Transactional
	public VendorProductActionResponse softDeleteOwnedProduct(Long productId) {
		Product product = findOwnedProduct(productId);
		if (product.getStatus() == ProductStatus.BANNED) {
			throw new BadRequestException("Sản phẩm bị khóa không thể thay đổi trạng thái bởi Vendor");
		}
		if (orderRepository.countForProductInStatuses(productId, ACTIVE_ORDER_STATUSES) > 0) {
			throw new BadRequestException("Không thể xóa sản phẩm đang có đơn hàng được xử lý");
		}
		product.setStatus(ProductStatus.INACTIVE);
		flushOrConflict(product);
		return new VendorProductActionResponse(product.getId(), product.getStatus(), "Đã ẩn sản phẩm khỏi chi nhánh");
	}

	private Product findOwnedProduct(Long productId) {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) throw new ForbiddenException("Chỉ Vendor mới được quản lý sản phẩm");
		Product product = productRepository.findNotDeletedWithOwnerAndImages(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
		if (product.getShop() == null || product.getShop().getOwner() == null
				|| !currentUser.getId().equals(product.getShop().getOwner().getId())) {
			throw new ForbiddenException("Bạn không có quyền quản lý sản phẩm của chi nhánh khác");
		}
		return product;
	}

	private java.util.Optional<ProductImage> primaryImage(Product product) {
		return product.getImages().stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsMain()))
				.findFirst()
				.or(() -> product.getImages().stream().findFirst());
	}

	private void flushOrConflict(Product product) {
		try {
			productRepository.saveAndFlush(product);
		} catch (OptimisticLockingFailureException exception) {
			throw new BadRequestException(OPTIMISTIC_LOCK_MESSAGE);
		}
	}

	private void deleteAfterCommit(String publicId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCommit() { cloudinaryService.delete(publicId, "image"); }
		});
	}

	private void deleteAfterRollback(String publicId) {
		if (publicId == null || !TransactionSynchronizationManager.isSynchronizationActive()) return;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) cloudinaryService.delete(publicId, "image");
			}
		});
	}
}
