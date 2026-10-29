package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.CardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardSuggestionResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.CardMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

	private static final Long SERIES_ID = 1L;
	private static final Long CARD_ID = 7L;

	@Mock
	private CardRepository cardRepository;
	@Mock
	private SeriesRepository seriesRepository;
	@Mock
	private CardMapper cardMapper;
	@Mock
	private CloudinaryService cloudinaryService;

	@InjectMocks
	private CardServiceImpl service;

	private final CardRequest request =
			new CardRequest(SERIES_ID, "Pikachu", "025", Rarity.RARE, CardLanguage.ENGLISH);

	private final MockMultipartFile image =
			new MockMultipartFile("image", "pikachu.png", "image/png", new byte[] {1, 2, 3});

	private Series series(boolean active) {
		Series series = new Series();
		series.setId(SERIES_ID);
		series.setActive(active);
		return series;
	}

	private Card existingCard() {
		Card card = new Card();
		card.setId(CARD_ID);
		card.setSeries(series(true));
		card.setImageUrl("old-url");
		card.setImagePublicId("old-pid");
		return card;
	}

	// ------------------------------------------------------------ create

	@Test
	void create_seriesNotFound_throwsResourceNotFound() {
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(request, null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_seriesHidden_throwsConflict() {
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series(false)));

		assertThatThrownBy(() -> service.create(request, null)).isInstanceOf(ConflictException.class);

		verify(cardRepository, never()).save(any());
	}

	@Test
	void create_duplicateCard_throwsConflictAndDoesNotUpload() {
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series(true)));
		when(cardRepository.existsBySeriesIdAndCardNumberIgnoreCaseAndLanguage(
				SERIES_ID, "025", CardLanguage.ENGLISH)).thenReturn(true);

		assertThatThrownBy(() -> service.create(request, image)).isInstanceOf(ConflictException.class);

		// Trùng thì không được upload ảnh, tránh ảnh rác trên Cloudinary
		verify(cloudinaryService, never()).uploadImage(any(), any());
		verify(cardRepository, never()).save(any());
	}

	@Test
	void create_withImage_uploadsAndStoresUrlAndPublicId() {
		Card entity = new Card();
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series(true)));
		when(cardMapper.toEntity(request)).thenReturn(entity);
		when(cloudinaryService.uploadImage(eq(image), eq("tcgshop/cards")))
				.thenReturn(new UploadResult("new-url", "new-pid"));
		when(cardRepository.save(entity)).thenReturn(entity);

		service.create(request, image);

		assertThat(entity.getImageUrl()).isEqualTo("new-url");
		assertThat(entity.getImagePublicId()).isEqualTo("new-pid");
		assertThat(entity.isActive()).isTrue();
		assertThat(entity.getCardNumber()).isEqualTo("025");
	}

	@Test
	void create_withoutImage_doesNotUpload() {
		Card entity = new Card();
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series(true)));
		when(cardMapper.toEntity(request)).thenReturn(entity);
		when(cardRepository.save(entity)).thenReturn(entity);

		service.create(request, null);

		verify(cloudinaryService, never()).uploadImage(any(), any());
		assertThat(entity.getImageUrl()).isNull();
	}

	// ------------------------------------------------------------ update

	@Test
	void update_duplicateWithAnotherCard_throwsConflict() {
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard()));
		when(cardRepository.existsBySeriesIdAndCardNumberIgnoreCaseAndLanguageAndIdNot(
				SERIES_ID, "025", CardLanguage.ENGLISH, CARD_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.update(CARD_ID, request, image))
				.isInstanceOf(ConflictException.class);

		verify(cloudinaryService, never()).uploadImage(any(), any());
	}

	@Test
	void update_withNewImage_replacesImageAndDeletesOldOne() {
		Card card = existingCard();
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(cloudinaryService.uploadImage(eq(image), eq("tcgshop/cards")))
				.thenReturn(new UploadResult("new-url", "new-pid"));

		service.update(CARD_ID, request, image);

		assertThat(card.getImageUrl()).isEqualTo("new-url");
		assertThat(card.getImagePublicId()).isEqualTo("new-pid");
		// Không có transaction trong unit test nên ảnh cũ bị xóa ngay
		verify(cloudinaryService).delete("old-pid", "image");
	}

	@Test
	void update_withoutImage_keepsOldImageAndDoesNotCallCloudinary() {
		Card card = existingCard();
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		service.update(CARD_ID, request, null);

		assertThat(card.getImageUrl()).isEqualTo("old-url");
		assertThat(card.getImagePublicId()).isEqualTo("old-pid");
		verify(cloudinaryService, never()).uploadImage(any(), any());
		verify(cloudinaryService, never()).delete(any(), any());
	}

	@Test
	void update_cardNotFound_throwsResourceNotFound() {
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(CARD_ID, request, null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ------------------------------------------------------------ setActive, getById, searchForListing

	@Test
	void setActive_false_hidesCard() {
		Card card = existingCard();
		card.setActive(true);
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		service.setActive(CARD_ID, false);

		assertThat(card.isActive()).isFalse();
	}

	@Test
	void getById_notFound_throwsResourceNotFound() {
		when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(CARD_ID)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void searchForListing_mapsPageToListingResponses() {
		Card card = existingCard();
		CardListingResponse listing = new CardListingResponse(CARD_ID, "Pikachu", "025", "151",
				Rarity.RARE, CardLanguage.ENGLISH, "old-url");
		Pageable pageable = PageRequest.of(0, 20);
		when(cardRepository.findAll(ArgumentMatchers.<Specification<Card>>any(), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(card), pageable, 1));
		when(cardMapper.toListingResponse(card)).thenReturn(listing);

		PageResponse<CardListingResponse> result = service.searchForListing("pika", SERIES_ID, pageable);

		assertThat(result.content()).containsExactly(listing);
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void searchPublicSuggestions_returnsActiveCatalogDetailsAndCapsResults() {
		Game game = new Game();
		game.setName("Pokemon");
		Series series = series(true);
		series.setName("151");
		series.setGame(game);
		Card card = existingCard();
		card.setName("Pikachu");
		card.setCardNumber("025");
		card.setLanguage(CardLanguage.ENGLISH);
		card.setSeries(series);
		when(cardRepository.findPublicSuggestions("Pika", PageRequest.of(0, 8)))
				.thenReturn(List.of(card));

		List<CardSuggestionResponse> result = service.searchPublicSuggestions(" Pika ");

		assertThat(result).containsExactly(new CardSuggestionResponse(
				CARD_ID, "Pikachu", "025", "151", "Pokemon", CardLanguage.ENGLISH));
	}

	@Test
	void searchPublicSuggestions_rejectsShortKeyword() {
		assertThatThrownBy(() -> service.searchPublicSuggestions("P"))
				.isInstanceOf(BadRequestException.class);
	}
}