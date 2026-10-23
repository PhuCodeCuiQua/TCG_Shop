package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorSingleProductRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.dto.response.VendorSingleProductResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.SeriesMapper;
import com.tcgshop.mapper.VendorSingleProductMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorSingleProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CardService;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.service.VendorSingleProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
public class VendorSingleProductServiceImpl implements VendorSingleProductService {

	private static final int CARD_VERIFY_PAGE_SIZE = 100;
	private static final String PRODUCT_IMAGE_FOLDER = "tcg-shop/products";

	private final CardService cardService;
	private final CardRepository cardRepository;
	private final SeriesRepository seriesRepository;
	private final SeriesMapper seriesMapper;
	private final ShopRepository shopRepository;
	private final VendorSingleProductRepository vendorSingleProductRepository;
	private final VendorSingleProductMapper vendorSingleProductMapper;
	private final CloudinaryService cloudinaryService;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Page<CardListingResponse> searchCards(String keyword, Long seriesId, Pageable pageable) {
		ensureVendor();
		PageResponse<CardListingResponse> response = cardService.searchForListing(normalize(keyword), seriesId, pageable);
		return new PageImpl<>(response.content(), pageable, response.totalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public CardListingResponse getListingCard(Long cardId) {
		ensureVendor();
		CardResponse card = cardService.getById(cardId);
		if (!card.active()) {
			throw new ResourceNotFoundException("Không tìm thấy thẻ đang được phép đăng bán");
		}

		PageResponse<CardListingResponse> listingCards = cardService.searchForListing(
				card.cardNumber(), card.seriesId(), PageRequest.of(0, CARD_VERIFY_PAGE_SIZE));
		return listingCards.content().stream()
				.filter(candidate -> candidate.id().equals(cardId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Thẻ không còn khả dụng để đăng bán"));
	}

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
	public VendorSingleProductResponse createListing(VendorSingleProductRequest request) {
		ensureVendor();
		Shop shop = findCurrentShop();
		if (shop.getStatus() != ShopStatus.APPROVED) {
			throw new ForbiddenException("Chỉ shop đã được duyệt mới có thể đăng bán sản phẩm");
		}
		CardListingResponse cardDetails = getListingCard(request.getCardId());
		Card card = cardRepository.findById(cardDetails.id())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ"));

		String edition = normalize(request.getEdition());
		if (vendorSingleProductRepository.existsActiveListingVariant(
				shop.getId(), card.getId(), request.getCondition(), request.getLanguage(), edition)) {
			throw new ConflictException(
					"Shop đã đăng bán thẻ này với cùng tình trạng, ngôn ngữ và phiên bản. "
							+ "Hãy cập nhật tồn kho của sản phẩm hiện có thay vì tạo sản phẩm mới.");
		}

		Product product = new Product();
		product.setShop(shop);
		product.setCard(card);
		product.setType(ProductType.SINGLE);
		product.setName(card.getName());
		product.setCardCondition(request.getCondition());
		product.setLanguage(request.getLanguage());
		product.setEdition(edition);
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

		Product savedProduct = vendorSingleProductRepository.save(product);
		return vendorSingleProductMapper.toResponse(savedProduct);
	}

	private Shop findCurrentShop() {
		return shopRepository.findByOwnerId(currentUser.getId())
				.filter(shop -> !Boolean.TRUE.equals(shop.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop của tài khoản hiện tại"));
	}

	private void ensureVendor() {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ người bán mới được đăng sản phẩm");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
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
