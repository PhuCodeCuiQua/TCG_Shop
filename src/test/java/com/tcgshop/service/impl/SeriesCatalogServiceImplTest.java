package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.SeriesRequest;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Series;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.SeriesMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.SeriesRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeriesCatalogServiceImplTest {

	private static final Long GAME_ID = 1L;
	private static final Long SERIES_ID = 5L;

	@Mock
	private SeriesRepository seriesRepository;
	@Mock
	private GameRepository gameRepository;
	@Mock
	private CardRepository cardRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private SeriesMapper seriesMapper;

	@InjectMocks
	private SeriesCatalogServiceImpl service;

	private final SeriesRequest request =
			new SeriesRequest(GAME_ID, "Scarlet & Violet", "SV1", LocalDate.of(2023, 3, 31));

	private Game game(boolean active) {
		Game game = new Game();
		game.setId(GAME_ID);
		game.setActive(active);
		return game;
	}

	private Series series() {
		Series series = new Series();
		series.setId(SERIES_ID);
		series.setActive(true);
		return series;
	}

	@Test
	void create_gameNotFound_throwsResourceNotFound() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_gameHidden_throwsConflict() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game(false)));

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

		verify(seriesRepository, never()).save(any());
	}

	@Test
	void create_duplicateNameInSameGame_throwsConflict() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game(true)));
		when(seriesRepository.existsByGameIdAndNameIgnoreCase(GAME_ID, "Scarlet & Violet")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

		verify(seriesRepository, never()).save(any());
	}

	@Test
	void create_duplicateCodeInSameGame_throwsConflict() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game(true)));
		when(seriesRepository.existsByGameIdAndCodeIgnoreCase(GAME_ID, "SV1")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

		verify(seriesRepository, never()).save(any());
	}

	@Test
	void create_validRequest_savesActiveSeriesLinkedToGame() {
		Game game = game(true);
		Series entity = new Series();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(seriesMapper.toEntity(request)).thenReturn(entity);
		when(seriesRepository.save(entity)).thenReturn(entity);

		service.create(request);

		assertThat(entity.getGame()).isSameAs(game);
		assertThat(entity.isActive()).isTrue();
		verify(seriesRepository).save(entity);
	}

	@Test
	void removeOrHide_seriesHasCards_hidesInsteadOfDeleting() {
		Series series = series();
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series));
		when(cardRepository.existsBySeriesId(SERIES_ID)).thenReturn(true);

		boolean deleted = service.removeOrHide(SERIES_ID);

		assertThat(deleted).isFalse();
		assertThat(series.isActive()).isFalse();
		verify(seriesRepository, never()).delete(any());
	}

	@Test
	void removeOrHide_seriesHasProducts_hidesInsteadOfDeleting() {
		Series series = series();
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series));
		when(productRepository.existsBySeriesId(SERIES_ID)).thenReturn(true);

		boolean deleted = service.removeOrHide(SERIES_ID);

		assertThat(deleted).isFalse();
		assertThat(series.isActive()).isFalse();
		verify(seriesRepository, never()).delete(any());
	}

	@Test
	void removeOrHide_noRelatedData_deletesPermanently() {
		Series series = series();
		when(seriesRepository.findById(SERIES_ID)).thenReturn(Optional.of(series));

		boolean deleted = service.removeOrHide(SERIES_ID);

		assertThat(deleted).isTrue();
		verify(seriesRepository).delete(series);
	}
}