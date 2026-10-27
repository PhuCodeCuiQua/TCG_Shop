package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ReviewSubmitRequest;
import com.tcgshop.dto.request.ReviewMediaRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ReviewResponse;
import com.tcgshop.dto.request.VendorReviewReplyRequest;
import com.tcgshop.dto.response.ReviewReplyResponse;
import com.tcgshop.dto.response.ReviewReplyMessageResponse;
import com.tcgshop.dto.response.ReviewSubmitResponse;
import com.tcgshop.dto.response.ReviewSummaryResponse;
import com.tcgshop.dto.response.ReviewProductResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Review;
import com.tcgshop.entity.ReviewMedia;
import com.tcgshop.entity.ReviewReply;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.MediaType;
import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ReviewMapper;
import com.tcgshop.repository.PublicReviewRepository;
import com.tcgshop.repository.ReviewAggregateRepository;
import com.tcgshop.repository.ReviewEligibilityRepository;
import com.tcgshop.repository.ReviewProductRepository;
import com.tcgshop.repository.ReviewRatingAggregate;
import com.tcgshop.repository.ReviewMediaListingRepository;
import com.tcgshop.repository.ReviewMediaListingRow;
import com.tcgshop.repository.ReviewRepository;
import com.tcgshop.repository.ReviewReplyRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.event.ReviewCreatedEvent;
import com.tcgshop.service.ReviewEligibilityService;
import com.tcgshop.service.NotificationService;
import com.tcgshop.service.ReviewService;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
	private static final int MAX_COMMENT_LENGTH = 2000;
	private static final int MAX_IMAGES = 5;
	private static final String REVIEW_MEDIA_FOLDER = "tcgshop/reviews";

	private final PublicReviewRepository publicReviewRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewReplyRepository reviewReplyRepository;
	private final ReviewAggregateRepository aggregateRepository;
	private final ReviewEligibilityRepository eligibilityRepository;
	private final ReviewProductRepository productRepository;
	private final UserRepository userRepository;
	private final ReviewMediaListingRepository mediaListingRepository;
	private final ReviewEligibilityService eligibilityService;
	private final ReviewMapper reviewMapper;
	private final CurrentUser currentUser;
	private final CloudinaryService cloudinaryService;
	private final ApplicationEventPublisher eventPublisher;
	private final NotificationService notificationService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
		return getReviewsPage(productId, null, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Integer rating, Pageable pageable) {
		if (rating != null && (rating < 1 || rating > 5)) {
			throw new BadRequestException("Bộ lọc đánh giá phải từ 1 đến 5 sao");
		}
		return getReviewsPage(productId, rating, pageable);
	}

	private PageResponse<ReviewResponse> getReviewsPage(Long productId, Integer rating, Pageable pageable) {
		productRepository.findPublicProduct(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
		Page<Review> reviews = publicReviewRepository
				.findVisibleByProductAndRating(productId, rating, pageable);
		List<Long> reviewIds = reviews.getContent().stream().map(Review::getId).toList();
		Map<Long, List<com.tcgshop.dto.response.ReviewMediaResponse>> mediaByReview = new HashMap<>();
		Map<Long, List<ReviewReplyMessageResponse>> repliesByReview = new HashMap<>();
		if (!reviewIds.isEmpty()) {
			for (ReviewMediaListingRow row : mediaListingRepository.findForReviews(reviewIds)) {
				mediaByReview.computeIfAbsent(row.getReviewId(), ignored -> new ArrayList<>())
						.add(reviewMapper.toMediaResponse(row));
			}
			for (ReviewReply reply : reviewReplyRepository.findByReviewIdInOrderByCreatedAtAscIdAsc(reviewIds)) {
				Review review = reply.getReview();
				Long senderId = reply.getSender().getId();
				String senderRole = review.getUser() != null && senderId.equals(review.getUser().getId())
						? "CUSTOMER" : "VENDOR";
				repliesByReview.computeIfAbsent(review.getId(), ignored -> new ArrayList<>())
						.add(new ReviewReplyMessageResponse(reply.getId(), senderId,
								reply.getSender().getFullName(), senderRole, reply.getContent(), reply.getCreatedAt()));
			}
		}
		return PageResponse.from(reviews, review -> {
			List<ReviewReplyMessageResponse> replies = new ArrayList<>();
			if (review.getVendorReply() != null && !review.getVendorReply().isBlank()) {
				var shop = review.getProduct() == null ? null : review.getProduct().getShop();
				Long vendorId = shop == null || shop.getOwner() == null ? null : shop.getOwner().getId();
				replies.add(new ReviewReplyMessageResponse(null, vendorId,
						shop == null ? "Người bán" : shop.getName(), "VENDOR", review.getVendorReply(), review.getRepliedAt()));
			}
			replies.addAll(repliesByReview.getOrDefault(review.getId(), List.of()));
			return reviewMapper.toResponse(review, mediaByReview.getOrDefault(review.getId(), List.of()), replies);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public boolean canCurrentVendorReplyToProduct(Long productId) {
		if (!currentUser.isAuthenticated() || !currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			return false;
		}
		Product product = productRepository.findPublicProduct(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
		return product.getShop() != null
				&& product.getShop().getOwner() != null
				&& currentUser.getId().equals(product.getShop().getOwner().getId());
	}

	@Override
	@Transactional
	public ReviewReplyResponse replyToReview(Long reviewId, VendorReviewReplyRequest request) {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR) && !currentUser.hasRole(RoleName.ROLE_USER)) {
			throw new ForbiddenException("Tài khoản không được phép tham gia hội thoại đánh giá");
		}
		if (request == null || request.reply() == null || request.reply().isBlank()) {
			throw new BadRequestException("Nội dung phản hồi không được để trống");
		}
		String reply = request.reply().trim();
		if (reply.length() > 2000) {
			throw new BadRequestException("Phản hồi tối đa 2.000 ký tự");
		}

		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Đánh giá", reviewId));
		if (review.isDeleted()) {
			throw new ResourceNotFoundException("Đánh giá", reviewId);
		}
		Long ownerId = review.getProduct() == null || review.getProduct().getShop() == null
				|| review.getProduct().getShop().getOwner() == null ? null
				: review.getProduct().getShop().getOwner().getId();
		boolean isVendor = currentUser.hasRole(RoleName.ROLE_VENDOR)
				&& ownerId != null && ownerId.equals(currentUser.getId());
		boolean isReviewer = currentUser.hasRole(RoleName.ROLE_USER)
				&& review.getUser() != null && currentUser.getId().equals(review.getUser().getId());
		if (!isVendor && !isReviewer) {
			throw new ForbiddenException("Chỉ người mua và chủ shop được tham gia hội thoại này");
		}

		User sender = isVendor ? review.getProduct().getShop().getOwner() : review.getUser();
		ReviewReply entity = new ReviewReply();
		entity.setReview(review);
		entity.setSender(sender);
		entity.setContent(reply);
		entity = reviewReplyRepository.saveAndFlush(entity);
		Long recipientId = isVendor ? review.getUser().getId() : ownerId;
		if (recipientId != null && !recipientId.equals(currentUser.getId())) {
			String content = isVendor ? "Người bán đã phản hồi đánh giá của bạn."
					: "Khách hàng đã trả lời cuộc trò chuyện đánh giá của bạn.";
			notificationService.send(recipientId, NotificationType.REVIEW_REPLY,
					"Có tin nhắn mới trong đánh giá sản phẩm", content,
					"/products/" + review.getProduct().getId() + "#reviews");
		}
		ReviewReplyMessageResponse message = new ReviewReplyMessageResponse(entity.getId(), sender.getId(),
				sender.getFullName(), isVendor ? "VENDOR" : "CUSTOMER", entity.getContent(), entity.getCreatedAt());
		return new ReviewReplyResponse(review.getId(), message);
	}
	@Override
	@Transactional(readOnly = true)
	public ReviewProductResponse getProductSummary(Long productId) {
		Product product = productRepository.findPublicProduct(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
		return reviewMapper.toProductResponse(product);
	}

	@Override
	@Transactional
	public ReviewSubmitResponse submit(Long userId, Long productId, ReviewSubmitRequest request) {
		validateActor(userId);
		if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
			throw new BadRequestException("Đánh giá phải từ 1 đến 5 sao");
		}
		return createReview(userId, productId, request.rating(), request.comment(), List.of(), null);
	}

	@Override
	@Transactional
	public ReviewSubmitResponse submitWithMedia(Long userId, Long productId, ReviewMediaRequest request) {
		validateRequest(request);
		return createReview(userId, productId, request.getRating(), request.getComment(),
				safeImages(request.getImages()), request.getVideo());
	}

	@Override
	@Transactional
	public ReviewSubmitResponse update(Long userId, Long reviewId, ReviewMediaRequest request) {
		validateActor(userId);
		validateRequest(request);
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Đánh giá", reviewId));
		ensureOwner(userId, review);
		if (review.isDeleted()) {
			throw new ResourceNotFoundException("Đánh giá", reviewId);
		}
		List<MultipartFile> newImages = safeImages(request.getImages());
		boolean hasNewVideo = hasFile(request.getVideo());
		validateAttachmentLimits(review.getMedia(), newImages, hasNewVideo);

		review.setRating(request.getRating());
		review.setComment(normalizeComment(request.getComment()));
		appendMedia(review, newImages, request.getVideo());
		review = reviewRepository.saveAndFlush(review);
		ReviewSummaryResponse summary = updateProductRating(review.getProduct());
		return new ReviewSubmitResponse(review.getId(), summary, "Đánh giá đã được cập nhật.");
	}

	@Override
	@Transactional
	public ReviewSubmitResponse delete(Long userId, Long reviewId) {
		validateActor(userId);
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Đánh giá", reviewId));
		ensureOwner(userId, review);
		if (review.isDeleted()) {
			throw new ResourceNotFoundException("Đánh giá", reviewId);
		}
		Product product = review.getProduct();
		List<ReviewMedia> media = new ArrayList<>(review.getMedia());
		review.setDeleted(true);
		review.getMedia().clear();
		reviewRepository.saveAndFlush(review);
		ReviewSummaryResponse summary = updateProductRating(product);
		scheduleCloudinaryDeleteAfterCommit(media);
		return new ReviewSubmitResponse(review.getId(), summary, "Đánh giá và tệp đính kèm đã được xóa.");
	}

	private ReviewSubmitResponse createReview(Long userId, Long productId, Integer rating,
			String rawComment, List<MultipartFile> images, MultipartFile video) {
		if (rating == null || rating < 1 || rating > 5) {
			throw new BadRequestException("Đánh giá phải từ 1 đến 5 sao");
		}
		Product product = productRepository.findPublicProductForUpdate(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
		String comment = normalizeComment(rawComment);
		validateAttachmentLimits(List.of(), images, hasFile(video));
		Long orderId = eligibilityService.findEligibleOrderId(userId, productId);
		if (reviewRepository.existsByOrderIdAndProductId(orderId, productId)) {
			throw new BadRequestException("Bạn đã đánh giá sản phẩm trong đơn hàng này");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
		Review review = new Review();
		review.setProduct(product);
		review.setUser(user);
		Order order = eligibilityRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		review.setOrder(order);
		review.setRating(rating);
		review.setComment(comment);
		review.setDeleted(false);
		appendMedia(review, images, video);
		review = reviewRepository.saveAndFlush(review);

		ReviewSummaryResponse summary = updateProductRating(product);
		if (product.getShop() != null && product.getShop().getOwner() != null) {
			eventPublisher.publishEvent(new ReviewCreatedEvent(
					product.getShop().getOwner().getId(), product.getId(), product.getName(), rating));
		}
		return new ReviewSubmitResponse(review.getId(), summary, "Cảm ơn bạn đã đánh giá sản phẩm!");
	}

	private void validateRequest(ReviewMediaRequest request) {
		validateActor(currentUser.getId());
		if (request == null || request.getRating() == null
				|| request.getRating() < 1 || request.getRating() > 5) {
			throw new BadRequestException("Đánh giá phải từ 1 đến 5 sao");
		}
		normalizeComment(request.getComment());
	}

	private void appendMedia(Review review, List<MultipartFile> images, MultipartFile video) {
		for (MultipartFile image : images) {
			if (!hasFile(image)) continue;
			UploadResult uploaded = cloudinaryService.uploadImage(image, REVIEW_MEDIA_FOLDER);
		scheduleCloudinaryCleanupOnRollback(uploaded.publicId(), "image");
			ReviewMedia media = new ReviewMedia();
			media.setReview(review);
			media.setMediaUrl(uploaded.url());
			media.setMediaPublicId(uploaded.publicId());
			media.setMediaType(MediaType.IMAGE);
			review.getMedia().add(media);
		}
		if (hasFile(video)) {
			UploadResult uploaded = cloudinaryService.uploadVideo(video, REVIEW_MEDIA_FOLDER);
			scheduleCloudinaryCleanupOnRollback(uploaded.publicId(), "video");
			ReviewMedia media = new ReviewMedia();
			media.setReview(review);
			media.setMediaUrl(uploaded.url());
			media.setMediaPublicId(uploaded.publicId());
			media.setMediaType(MediaType.VIDEO);
			review.getMedia().add(media);
		}
	}

	private void validateAttachmentLimits(Collection<ReviewMedia> existing,
			List<MultipartFile> images, boolean newVideo) {
		long existingImages = existing.stream().filter(media -> media.getMediaType() == MediaType.IMAGE).count();
		long existingVideos = existing.stream().filter(media -> media.getMediaType() == MediaType.VIDEO).count();
		long newImageCount = images.stream().filter(this::hasFile).count();
		if (existingImages + newImageCount > MAX_IMAGES) {
			throw new BadRequestException("Mỗi đánh giá được đính kèm tối đa 5 ảnh");
		}
		if (existingVideos + (newVideo ? 1 : 0) > 1) {
			throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm 1 video");
		}
	}

	private List<MultipartFile> safeImages(List<MultipartFile> images) {
		return images == null ? List.of() : images;
	}

	private boolean hasFile(MultipartFile file) {
		return file != null && !file.isEmpty();
	}

	private void ensureOwner(Long userId, Review review) {
		if (review.getUser() == null || !userId.equals(review.getUser().getId())) {
			throw new ForbiddenException("Bạn không có quyền chỉnh sửa đánh giá của người khác");
		}
	}

	private void scheduleCloudinaryCleanupOnRollback(String publicId, String resourceType) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) deleteCloudinaryQuietly(publicId, resourceType);
			}
		});
	}

	private void scheduleCloudinaryDeleteAfterCommit(List<ReviewMedia> media) {
		if (media.isEmpty()) return;
		Runnable cleanup = () -> media.forEach(item -> deleteCloudinaryQuietly(item.getMediaPublicId(),
				item.getMediaType() == MediaType.VIDEO ? "video" : "image"));
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			cleanup.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cleanup.run();
			}
		});
	}

	private void deleteCloudinaryQuietly(String publicId, String resourceType) {
		if (publicId == null || publicId.isBlank()) return;
		try {
			cloudinaryService.delete(publicId, resourceType);
		} catch (RuntimeException exception) {
			log.warn("Không thể xóa review media trên Cloudinary: {}", exception.getMessage());
		}
	}

	private ReviewSummaryResponse updateProductRating(Product product) {
		Optional<ReviewRatingAggregate> aggregate = aggregateRepository.summarizeByProductId(product.getId());
		ReviewRatingAggregate values = aggregate.orElse(null);
		int count = values == null || values.getReviewCount() == null ? 0 : values.getReviewCount().intValue();
		BigDecimal average = values == null || values.getAverageRating() == null || count == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(values.getAverageRating()).setScale(2, RoundingMode.HALF_UP);
		product.setAvgRating(average);
		product.setRatingCount(count);
		return new ReviewSummaryResponse(average, count);
	}

	private String normalizeComment(String rawComment) {
		if (rawComment == null || rawComment.isBlank()) {
			return null;
		}
		String comment = rawComment.trim();
		if (comment.length() > MAX_COMMENT_LENGTH) {
			throw new BadRequestException("Nhận xét không được vượt quá 2.000 ký tự");
		}
		return comment;
	}

	private void validateActor(Long userId) {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER) || userId == null || !userId.equals(currentUser.getId())) {
			throw new ForbiddenException("Không thể gửi đánh giá thay cho tài khoản khác");
		}
	}
}


