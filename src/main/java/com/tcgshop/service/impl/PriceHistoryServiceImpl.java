package com.tcgshop.service.impl;

import com.tcgshop.dto.request.PriceHistoryEditRequest;
import com.tcgshop.dto.request.PriceHistoryRequest;
import com.tcgshop.dto.response.CardPriceOptionResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PriceHistoryResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.event.PriceUpdatedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.PriceHistoryMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PriceHistoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class PriceHistoryServiceImpl implements PriceHistoryService {

	private final PriceHistoryRepository priceHistoryRepository;
	private final CardRepository cardRepository;
	private final PriceHistoryMapper priceHistoryMapper;
	private final ApplicationEventPublisher eventPublisher;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public List<CardPriceOptionResponse> getActiveCards() {
		ensureManagerOrAdmin();
		return cardRepository.findByActiveTrueOrderByNameAsc().stream()
				.map(priceHistoryMapper::toOption)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PriceHistoryResponse> search(
			Long cardId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
		ensureManagerOrAdmin();
		validateDateRange(startDate, endDate);
		if (cardId == null) {
			return PageResponse.from(Page.empty(pageable));
		}
		return PageResponse.from(
				priceHistoryRepository.searchByCardAndDateRange(cardId, startDate, endDate, pageable),
				priceHistoryMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public PriceHistoryResponse getById(Long historyId) {
		ensureManagerOrAdmin();
		return priceHistoryMapper.toResponse(findHistory(historyId));
	}

	@Override
	@Transactional
	public PriceHistoryResponse save(PriceHistoryRequest request) {
		ensureManagerOrAdmin();
		if (request == null) {
			throw new BadRequestException("Dữ liệu giá thị trường không hợp lệ");
		}
		if (request.getCardId() == null || request.getRecordedDate() == null) {
			throw new BadRequestException("Vui lòng chọn thẻ và ngày ghi nhận");
		}
		validatePositivePrice(request.getMarketPrice(), "Market price");
		validatePositivePrice(request.getAvgPrice(), "Avg price");
		Card card = cardRepository.findById(request.getCardId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ"));
		if (!card.isActive()) {
			throw new ConflictException("Không thể nhập giá cho thẻ đang bị ẩn");
		}

		PriceHistory history = priceHistoryRepository
				.findByCardIdAndRecordedDate(card.getId(), request.getRecordedDate())
				.orElse(null);
		if (history != null && !request.isOverwriteExisting()) {
			throw new ConflictException("Ngày này đã có lịch sử giá cho thẻ. Xác nhận ghi đè để cập nhật.");
		}

		BigDecimal oldMarketPrice = card.getMarketPrice();
		if (history == null) {
			history = new PriceHistory();
			history.setCard(card);
			history.setRecordedDate(request.getRecordedDate());
		}
		history.setMarketPrice(request.getMarketPrice());
		history.setAvgPrice(request.getAvgPrice());
		history.setSource("ADMIN");
		PriceHistory savedHistory = priceHistoryRepository.save(history);
		refreshCurrentMarketPrice(card, oldMarketPrice, request.getRecordedDate());
		return priceHistoryMapper.toResponse(savedHistory);
	}

	@Override
	@Transactional
	public PriceHistoryResponse update(Long historyId, PriceHistoryEditRequest request) {
		ensureManagerOrAdmin();
		if (request == null) {
			throw new BadRequestException("Dữ liệu giá thị trường không hợp lệ");
		}
		validatePositivePrice(request.getMarketPrice(), "Market price");
		validatePositivePrice(request.getAvgPrice(), "Avg price");
		PriceHistory history = findHistory(historyId);
		Card card = history.getCard();
		BigDecimal oldMarketPrice = card.getMarketPrice();
		history.setMarketPrice(request.getMarketPrice());
		history.setAvgPrice(request.getAvgPrice());
		PriceHistory savedHistory = priceHistoryRepository.save(history);
		refreshCurrentMarketPrice(card, oldMarketPrice, history.getRecordedDate());
		return priceHistoryMapper.toResponse(savedHistory);
	}

	@Override
	@Transactional
	public void delete(Long historyId) {
		ensureManagerOrAdmin();
		PriceHistory history = findHistory(historyId);
		Card card = history.getCard();
		BigDecimal oldMarketPrice = card.getMarketPrice();
		LocalDate affectedDate = history.getRecordedDate();
		priceHistoryRepository.delete(history);
		priceHistoryRepository.flush();
		refreshCurrentMarketPrice(card, oldMarketPrice, affectedDate);
	}

	private void refreshCurrentMarketPrice(Card card, BigDecimal oldMarketPrice, LocalDate affectedDate) {
		PriceHistory latestHistory = priceHistoryRepository
				.findTopByCardIdOrderByRecordedDateDesc(card.getId())
				.orElse(null);
		BigDecimal newMarketPrice = latestHistory == null ? null : latestHistory.getMarketPrice();
		card.setMarketPrice(newMarketPrice);
		eventPublisher.publishEvent(new PriceUpdatedEvent(
				card.getId(),
				card.getName(),
				oldMarketPrice,
				newMarketPrice,
				latestHistory == null ? affectedDate : latestHistory.getRecordedDate(),
				latestHistory == null ? "ADMIN" : latestHistory.getSource()));
	}

	private PriceHistory findHistory(Long historyId) {
		return priceHistoryRepository.findById(historyId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử giá"));
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BadRequestException("Ngày bắt đầu không được sau ngày kết thúc");
		}
	}

	private void validatePositivePrice(BigDecimal price, String fieldName) {
		if (price == null || price.signum() <= 0) {
			throw new BadRequestException(fieldName + " phải lớn hơn 0");
		}
		if (price.scale() > 2 || price.precision() - price.scale() > 13) {
			throw new BadRequestException(fieldName + " tối đa 13 chữ số và 2 chữ số thập phân");
		}
	}

	private void ensureManagerOrAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER)
				&& !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền quản lý lịch sử giá");
		}
	}
}
