package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorOtherProductRequest;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.SeriesMapper;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.service.VendorOtherProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
public class VendorOtherProductServiceImpl implements VendorOtherProductService {

	private static final String PRODUCT_IMAGE_FOLDER = "tcg-shop/products";
	private static final Map<String, String> SUPPLY_CATEGORIES = Map.of(
			"SLEEVES", "Sleeves",
			"DECK_BOX", "Deck Box",
			"PLAYMAT", "Playmat",
			"OTHER", "Phụ kiện khác");
	private static final List<ProductType> SELLABLE_TYPES = List.of(
			ProductType.BOOSTER_BOX,
			ProductType.BOOSTER_PACK,
			ProductType.BUNDLE,
			ProductType.SUPPLY);

	private final CurrentUser currentUser;
	private final ShopRepository shopRepository;
	private final SeriesRepository seriesRepository;
	private final ProductRepository productRepository;
	private final SeriesMapper seriesMapper;
	private final CloudinaryService cloudinaryService;

	@Override
	@Transactional(readOnly = true)
	public List<SeriesResponse> getActiveSeriesOptions() {
		ensureVendor();
		return seriesRepository.findActivePromotionOptions().stream()
				.map(seriesMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public void createProduct(VendorOtherProductRequest request) {
		ensureVendor();
		validateRequest(request);

		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop của tài khoản hiện tại"));
		if (shop.getStatus() != ShopStatus.APPROVED) {
			throw new ForbiddenException("Chỉ shop đã được duyệt mới có thể đăng bán sản phẩm");
		}

		ProductType type = request.getType();
		Series series = isSealedType(type) ? findActiveSeries(request.getSeriesId()) : null;
		Product product = new Product();
		product.setShop(shop);
		product.setType(type);
		product.setSeries(series);
		product.setName(request.getName().trim());
		product.setDescription(buildDescription(request));
		product.setLanguage(isSealedType(type) ? request.getLanguage() : null);
		product.setCard(null);
		product.setCardCondition(null);
		product.setEdition(null);
		product.setPrice(request.getPrice());
		product.setStock(request.getStock());
		product.setSoldCount(0);
		product.setRatingCount(0);
		product.setFavoriteCount(0);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);

		MultipartFile image = request.getImage();
		if (image != null && !image.isEmpty()) {
			UploadResult uploaded = cloudinaryService.uploadImage(image, PRODUCT_IMAGE_FOLDER);
			scheduleUploadedImageCleanup(uploaded.publicId());
			ProductImage productImage = new ProductImage();
			productImage.setProduct(product);
			productImage.setImageUrl(uploaded.url());
			productImage.setImagePublicId(uploaded.publicId());
			productImage.setIsMain(true);
			productImage.setDisplayOrder(0);
			product.getImages().add(productImage);
		}

		productRepository.save(product);
	}

	private void validateRequest(VendorOtherProductRequest request) {
		if (request == null) {
			throw new BadRequestException("Dữ liệu sản phẩm không hợp lệ");
		}
		if (request.getType() == null || !SELLABLE_TYPES.contains(request.getType())) {
			throw new BadRequestException("Vui lòng chọn loại sản phẩm niêm phong hoặc phụ kiện hợp lệ");
		}
		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Vui lòng nhập tên sản phẩm");
		}
		if (request.getDescription() == null || request.getDescription().isBlank()) {
			throw new BadRequestException("Vui lòng nhập mô tả sản phẩm");
		}
		if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BadRequestException("Giá bán phải lớn hơn 0");
		}
		if (request.getStock() == null || request.getStock() <= 0) {
			throw new BadRequestException("Số lượng phải lớn hơn 0");
		}
		if (isSealedType(request.getType())) {
			if (request.getSeriesId() == null) {
				throw new BadRequestException("Vui lòng chọn series của sản phẩm");
			}
			if (request.getLanguage() == null) {
				throw new BadRequestException("Vui lòng chọn ngôn ngữ sản phẩm");
			}
		} else if (!SUPPLY_CATEGORIES.containsKey(request.getAccessoryCategory())) {
			throw new BadRequestException("Vui lòng chọn loại phụ kiện hợp lệ");
		}
	}

	private Series findActiveSeries(Long seriesId) {
		Series series = seriesRepository.findById(seriesId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy series"));
		if (!series.isActive() || !series.getGame().isActive()) {
			throw new BadRequestException("Series đã bị ẩn hoặc không còn khả dụng");
		}
		return series;
	}

	private String buildDescription(VendorOtherProductRequest request) {
		String description = request.getDescription().trim();
		if (request.getType() != ProductType.SUPPLY) {
			return description;
		}
		String categoryLabel = SUPPLY_CATEGORIES.get(request.getAccessoryCategory());
		return "Nhóm phụ kiện: " + categoryLabel + "\n\n" + description;
	}

	private boolean isSealedType(ProductType type) {
		return type == ProductType.BOOSTER_BOX
				|| type == ProductType.BOOSTER_PACK
				|| type == ProductType.BUNDLE;
	}

	private void ensureVendor() {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ tài khoản người bán mới được đăng sản phẩm");
		}
	}

	private void scheduleUploadedImageCleanup(String publicId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					try {
						cloudinaryService.delete(publicId, "image");
					} catch (RuntimeException exception) {
						log.warn("Không thể xóa ảnh sản phẩm {} sau khi transaction rollback", publicId, exception);
					}
				}
			}
		});
	}
}
