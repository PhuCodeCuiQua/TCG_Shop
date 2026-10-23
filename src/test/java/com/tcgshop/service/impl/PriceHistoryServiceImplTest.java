package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.PriceHistoryEditRequest;
import com.tcgshop.dto.request.PriceHistoryRequest;
import com.tcgshop.dto.response.PriceHistoryResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.event.PriceUpdatedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.PriceHistoryMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceImplTest {

	@Mock private PriceHistoryRepository priceHistoryRepository;
	@Mock private CardRepository cardRepository;
	@Mock private PriceHistoryMapper priceHistoryMapper;
	@Mock private ApplicationEventPublisher eventPublisher;
	@Mock private CurrentUser currentUser;
	@InjectMocks private PriceHistoryServiceImpl service;

	@Test
	void saveCreatesHistoryUpdatesCurrentPriceAndPublishesEvent() {
		Card card = card(new BigDecimal("100.00"));
		PriceHistory saved = history(card, LocalDate.of(2026, 10, 23), "150.00", "140.00");
		PriceHistoryResponse response = response(saved);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(cardRepository.findById(7L)).thenReturn(Optional.of(card));
		when(priceHistoryRepository.findByCardIdAndRecordedDate(7L, saved.getRecordedDate()))
				.thenReturn(Optional.empty());
		when(priceHistoryRepository.save(any(PriceHistory.class))).thenReturn(saved);
		when(priceHistoryRepository.findTopByCardIdOrderByRecordedDateDesc(7L)).thenReturn(Optional.of(saved));
		when(priceHistoryMapper.toResponse(saved)).thenReturn(response);

		PriceHistoryResponse result = service.save(request(false, "150.00", "140.00"));

		assertThat(result).isEqualTo(response);
		assertThat(card.getMarketPrice()).isEqualByComparingTo("150.00");
		ArgumentCaptor<PriceUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PriceUpdatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue()).isEqualTo(new PriceUpdatedEvent(
				7L, "Pikachu", new BigDecimal("100.00"), new BigDecimal("150.00"),
				LocalDate.of(2026, 10, 23), "ADMIN"));
	}

	@Test
	void saveRejectsDuplicateUnlessOverwriteWasExplicitlyConfirmed() {
		Card card = card(new BigDecimal("100.00"));
		PriceHistory existing = history(card, LocalDate.of(2026, 10, 23), "120.00", "115.00");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(cardRepository.findById(7L)).thenReturn(Optional.of(card));
		when(priceHistoryRepository.findByCardIdAndRecordedDate(7L, existing.getRecordedDate()))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.save(request(false, "150.00", "140.00")))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("Xác nhận ghi đè");

		verify(priceHistoryRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void saveOverwritesExistingHistoryAndPublishesUpdatedPrice() {
		Card card = card(new BigDecimal("120.00"));
		PriceHistory existing = history(card, LocalDate.of(2026, 10, 23), "120.00", "115.00");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(cardRepository.findById(7L)).thenReturn(Optional.of(card));
		when(priceHistoryRepository.findByCardIdAndRecordedDate(7L, existing.getRecordedDate()))
				.thenReturn(Optional.of(existing));
		when(priceHistoryRepository.save(existing)).thenReturn(existing);
		when(priceHistoryRepository.findTopByCardIdOrderByRecordedDateDesc(7L)).thenReturn(Optional.of(existing));
		when(priceHistoryMapper.toResponse(existing)).thenReturn(response(existing));

		service.save(request(true, "150.00", "145.00"));

		assertThat(existing.getMarketPrice()).isEqualByComparingTo("150.00");
		assertThat(existing.getAvgPrice()).isEqualByComparingTo("145.00");
		assertThat(card.getMarketPrice()).isEqualByComparingTo("150.00");
		verify(eventPublisher).publishEvent(any(PriceUpdatedEvent.class));
	}

	@Test
	void saveHistoricalDateKeepsLatestMarketPrice() {
		Card card = card(new BigDecimal("200.00"));
		LocalDate oldDate = LocalDate.of(2026, 10, 20);
		PriceHistory inserted = history(card, oldDate, "150.00", "140.00");
		PriceHistory latest = history(card, LocalDate.of(2026, 10, 23), "200.00", "190.00");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(cardRepository.findById(7L)).thenReturn(Optional.of(card));
		when(priceHistoryRepository.findByCardIdAndRecordedDate(7L, oldDate)).thenReturn(Optional.empty());
		when(priceHistoryRepository.save(any(PriceHistory.class))).thenReturn(inserted);
		when(priceHistoryRepository.findTopByCardIdOrderByRecordedDateDesc(7L)).thenReturn(Optional.of(latest));
		when(priceHistoryMapper.toResponse(inserted)).thenReturn(response(inserted));

		PriceHistoryRequest request = request(false, "150.00", "140.00");
		request.setRecordedDate(oldDate);
		service.save(request);

		assertThat(card.getMarketPrice()).isEqualByComparingTo("200.00");
		verify(eventPublisher).publishEvent(new PriceUpdatedEvent(
				7L, "Pikachu", new BigDecimal("200.00"), new BigDecimal("200.00"),
				LocalDate.of(2026, 10, 23), "ADMIN"));
	}

	@Test
	void updateRejectsNonPositivePrices() {
		PriceHistoryEditRequest request = new PriceHistoryEditRequest();
		request.setMarketPrice(BigDecimal.ZERO);
		request.setAvgPrice(new BigDecimal("10.00"));
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);

		assertThatThrownBy(() -> service.update(3L, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("lớn hơn 0");

		verify(priceHistoryRepository, never()).findById(3L);
	}

	@Test
	void deleteRecalculatesCurrentPriceFromRemainingLatestHistory() {
		Card card = card(new BigDecimal("200.00"));
		PriceHistory history = history(card, LocalDate.of(2026, 10, 23), "200.00", "190.00");
		PriceHistory previous = history(card, LocalDate.of(2026, 10, 22), "180.00", "175.00");
		previous.setSource("JOB");
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(priceHistoryRepository.findById(3L)).thenReturn(Optional.of(history));
		when(priceHistoryRepository.findTopByCardIdOrderByRecordedDateDesc(7L)).thenReturn(Optional.of(previous));

		service.delete(3L);

		assertThat(card.getMarketPrice()).isEqualByComparingTo("180.00");
		verify(priceHistoryRepository).delete(history);
		verify(priceHistoryRepository).flush();
		verify(eventPublisher).publishEvent(new PriceUpdatedEvent(
				7L, "Pikachu", new BigDecimal("200.00"), new BigDecimal("180.00"),
				LocalDate.of(2026, 10, 22), "JOB"));
	}

	@Test
	void searchRejectsInvalidDateRangeBeforeQueryingRepository() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);

		assertThatThrownBy(() -> service.search(
				7L, LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 23), PageRequest.of(0, 20)))
				.isInstanceOf(BadRequestException.class);

		verify(priceHistoryRepository, never()).searchByCardAndDateRange(any(), any(), any(), any());
	}

	@Test
	void vendorCannotManagePriceHistory() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> service.getActiveCards())
				.isInstanceOf(ForbiddenException.class);

		verify(cardRepository, never()).findByActiveTrueOrderByNameAsc();
	}

	private PriceHistoryRequest request(boolean overwrite, String marketPrice, String avgPrice) {
		PriceHistoryRequest request = new PriceHistoryRequest();
		request.setCardId(7L);
		request.setRecordedDate(LocalDate.of(2026, 10, 23));
		request.setMarketPrice(new BigDecimal(marketPrice));
		request.setAvgPrice(new BigDecimal(avgPrice));
		request.setOverwriteExisting(overwrite);
		return request;
	}

	private Card card(BigDecimal marketPrice) {
		Card card = new Card();
		card.setId(7L);
		card.setName("Pikachu");
		card.setActive(true);
		card.setMarketPrice(marketPrice);
		return card;
	}

	private PriceHistory history(Card card, LocalDate date, String marketPrice, String avgPrice) {
		PriceHistory history = new PriceHistory();
		history.setId(3L);
		history.setCard(card);
		history.setRecordedDate(date);
		history.setMarketPrice(new BigDecimal(marketPrice));
		history.setAvgPrice(new BigDecimal(avgPrice));
		history.setSource("ADMIN");
		return history;
	}

	private PriceHistoryResponse response(PriceHistory history) {
		return new PriceHistoryResponse(history.getId(), history.getCard().getId(),
				history.getCard().getName(), null, null, history.getRecordedDate(),
				history.getMarketPrice(), history.getAvgPrice(), history.getSource());
	}
}
