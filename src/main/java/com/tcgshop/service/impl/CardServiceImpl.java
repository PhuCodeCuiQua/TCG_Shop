package com.tcgshop.service.impl;

import com.tcgshop.dto.request.CardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardResponse;
import com.tcgshop.dto.response.CardSuggestionResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.CardMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.CardSpecifications;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.service.CardService;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

	private static final String IMAGE_FOLDER = "tcgshop/cards";
	private static final String IMAGE_RESOURCE_TYPE = "image";

	private final CardRepository cardRepository;
	private final SeriesRepository seriesRepository;
	private final CardMapper cardMapper;
	private final CloudinaryService cloudinaryService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CardListingResponse> searchForListing(String keyword, Long seriesId,
			Pageable pageable) {
		return PageResponse.from(cardRepository
				.findAll(CardSpecifications.forListing(keyword, seriesId), pageable)
				.map(cardMapper::toListingResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardSuggestionResponse> searchPublicSuggestions(String keyword) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		if (normalizedKeyword.length() < 2 || normalizedKeyword.length() > 80) {
			throw new BadRequestException("Từ khóa phải có từ 2 đến 80 ký tự");
		}
		return cardRepository.findPublicSuggestions(normalizedKeyword, PageRequest.of(0, 8)).stream()
				.map(card -> new CardSuggestionResponse(
						card.getId(),
						card.getName(),
						card.getCardNumber(),
						card.getSeries().getName(),
						card.getSeries().getGame().getName(),
						card.getLanguage()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CardResponse> search(String keyword, Long gameId, Long seriesId, Rarity rarity,
			Boolean active, Pageable pageable) {
		return PageResponse.from(cardRepository
				.findAll(CardSpecifications.forManagement(keyword, gameId, seriesId, rarity, active), pageable)
				.map(cardMapper::toResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public CardResponse getById(Long id) {
		return cardMapper.toResponse(findOrThrow(id));
	}

	@Override
	@Transactional
	public CardResponse create(CardRequest request, MultipartFile image) {
		return create(request, image, true);
	}

	@Override
	@Transactional
	public CardResponse createPending(CardRequest request, MultipartFile image) {
		return create(request, image, false);
	}

	private CardResponse create(CardRequest request, MultipartFile image, boolean active) {
		Series series = findSeriesOrThrow(request.seriesId());
		if (!series.isActive()) {
			throw new ConflictException("Series đang bị ẩn, không thể thêm thẻ");
		}
		String cardNumber = request.cardNumber().trim();
		// Kiểm tra trùng TRƯỚC khi upload để không tạo ảnh rác trên Cloudinary
		if (cardRepository.existsBySeriesIdAndCardNumberIgnoreCaseAndLanguage(
				series.getId(), cardNumber, request.language())) {
			throw new ConflictException("Thẻ này đã tồn tại (trùng series, số thẻ và ngôn ngữ)");
		}

		Card card = cardMapper.toEntity(request);
		card.setSeries(series);
		card.setName(request.name().trim());
		card.setCardNumber(cardNumber);
		card.setActive(active);

		if (hasImage(image)) {
			UploadResult uploaded = cloudinaryService.uploadImage(image, IMAGE_FOLDER);
			card.setImageUrl(uploaded.url());
			card.setImagePublicId(uploaded.publicId());
			// Nếu transaction rollback thì xóa ảnh vừa upload
			scheduleImageCleanup(uploaded.publicId(), null);
		}
		return cardMapper.toResponse(cardRepository.save(card));
	}

	@Override
	@Transactional
	public CardResponse update(Long id, CardRequest request, MultipartFile image) {
		Card card = findOrThrow(id);

		Series series = card.getSeries();
		if (!request.seriesId().equals(series.getId())) {
			// Đổi sang series khác: series mới phải tồn tại và đang hiện
			series = findSeriesOrThrow(request.seriesId());
			if (!series.isActive()) {
				throw new ConflictException("Series đang bị ẩn, không thể chuyển thẻ sang series này");
			}
		}
		String cardNumber = request.cardNumber().trim();
		// Loại trừ chính thẻ đang sửa khi kiểm tra trùng
		if (cardRepository.existsBySeriesIdAndCardNumberIgnoreCaseAndLanguageAndIdNot(
				series.getId(), cardNumber, request.language(), id)) {
			throw new ConflictException("Thẻ này đã tồn tại (trùng series, số thẻ và ngôn ngữ)");
		}

		cardMapper.updateEntity(request, card);
		card.setSeries(series);
		card.setName(request.name().trim());
		card.setCardNumber(cardNumber);

		if (hasImage(image)) {
			String oldPublicId = card.getImagePublicId();
			UploadResult uploaded = cloudinaryService.uploadImage(image, IMAGE_FOLDER);
			card.setImageUrl(uploaded.url());
			card.setImagePublicId(uploaded.publicId());
			// Commit thành công thì xóa ảnh cũ; rollback thì xóa ảnh mới (giữ nguyên ảnh cũ)
			scheduleImageCleanup(uploaded.publicId(), oldPublicId);
		}
		return cardMapper.toResponse(card); // dirty checking tự UPDATE khi commit
	}

	@Override
	@Transactional
	public CardResponse setActive(Long id, boolean active) {
		Card card = findOrThrow(id);
		card.setActive(active);
		return cardMapper.toResponse(card);
	}

	private Card findOrThrow(Long id) {
		return cardRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ"));
	}

	private Series findSeriesOrThrow(Long seriesId) {
		return seriesRepository.findById(seriesId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy series"));
	}

	private boolean hasImage(MultipartFile image) {
		return image != null && !image.isEmpty();
	}

	/**
	 * Dọn ảnh theo kết quả transaction: commit thì xóa ảnh cũ, rollback thì xóa ảnh mới.
	 * Không có transaction (vd unit test) thì coi như đã thành công và xóa ảnh cũ ngay.
	 */
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

	// Lỗi xóa ảnh chỉ ghi log, không làm hỏng nghiệp vụ chính
	private void deleteImageQuietly(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		try {
			cloudinaryService.delete(publicId, IMAGE_RESOURCE_TYPE);
		} catch (Exception ex) {
			log.warn("Không thể xóa ảnh {} trên Cloudinary: {}", publicId, ex.getMessage());
		}
	}
}
