package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorShopManageRequest;
import com.tcgshop.dto.response.VendorShopManageResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorShopManageMapper;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.service.VendorShopManageService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorShopManageServiceImpl implements VendorShopManageService {
	private static final String IMAGE_FOLDER = "tcg-shop/shops";
	private static final String IMAGE_RESOURCE_TYPE = "image";

	private final ShopRepository shopRepository;
	private final VendorShopManageMapper vendorShopManageMapper;
	private final CloudinaryService cloudinaryService;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Optional<VendorShopManageResponse> getCurrentVendorShop() {
		return shopRepository.findByOwnerId(currentUser.getId()).map(vendorShopManageMapper::toResponse);
	}

	@Override
	@Transactional
	public VendorShopManageResponse updateCurrentVendorShop(VendorShopManageRequest request) {
		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop của bạn"));
		String oldLogoPublicId = shop.getLogoPublicId();
		String oldBannerPublicId = shop.getBannerPublicId();
		vendorShopManageMapper.updateShop(request, shop);

		if (hasFile(request.getLogo())) {
			UploadResult uploadedLogo = cloudinaryService.uploadImage(request.getLogo(), IMAGE_FOLDER);
			shop.setLogoUrl(uploadedLogo.url());
			shop.setLogoPublicId(uploadedLogo.publicId());
			scheduleImageCleanup(uploadedLogo.publicId(), oldLogoPublicId);
		}
		if (hasFile(request.getBanner())) {
			UploadResult uploadedBanner = cloudinaryService.uploadImage(request.getBanner(), IMAGE_FOLDER);
			shop.setBannerUrl(uploadedBanner.url());
			shop.setBannerPublicId(uploadedBanner.publicId());
			scheduleImageCleanup(uploadedBanner.publicId(), oldBannerPublicId);
		}

		return vendorShopManageMapper.toResponse(shopRepository.save(shop));
	}

	private boolean hasFile(MultipartFile file) {
		return file != null && !file.isEmpty();
	}

	/** Commit xong mới xóa ảnh cũ; rollback thì xóa ảnh mới để shop không trỏ vào file đã mất. */
	private void scheduleImageCleanup(String newPublicId, String oldPublicId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status == STATUS_COMMITTED) {
						deleteImageQuietly(oldPublicId);
					} else {
						deleteImageQuietly(newPublicId);
					}
				}
			});
		} else {
			deleteImageQuietly(oldPublicId);
		}
	}

	private void deleteImageQuietly(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		try {
			cloudinaryService.delete(publicId, IMAGE_RESOURCE_TYPE);
		} catch (Exception exception) {
			log.warn("Không thể dọn ảnh shop trên Cloudinary: {}", exception.getMessage());
		}
	}
}
