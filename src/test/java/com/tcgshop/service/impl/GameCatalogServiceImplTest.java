package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.GameRequest;
import com.tcgshop.entity.Game;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.GameMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.SeriesRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameCatalogServiceImplTest {

	private static final Long GAME_ID = 1L;

	@Mock
	private GameRepository gameRepository;
	@Mock
	private SeriesRepository seriesRepository;
	@Mock
	private GameMapper gameMapper;

	@InjectMocks
	private GameCatalogServiceImpl service;

	private final GameRequest request = new GameRequest("Pokémon", "POKEMON", "Mô tả");

	private Game activeGame() {
		Game game = new Game();
		game.setId(GAME_ID);
		game.setActive(true);
		return game;
	}

	@Test
	void create_duplicateName_throwsConflictAndDoesNotSave() {
		when(gameRepository.existsByNameIgnoreCase("Pokémon")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

		verify(gameRepository, never()).save(any());
	}

	@Test
	void create_duplicateCode_throwsConflictAndDoesNotSave() {
		when(gameRepository.existsByCodeIgnoreCase("POKEMON")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

		verify(gameRepository, never()).save(any());
	}

	@Test
	void create_validRequest_savesActiveGame() {
		Game entity = new Game();
		when(gameMapper.toEntity(request)).thenReturn(entity);
		when(gameRepository.save(entity)).thenReturn(entity);

		service.create(request);

		assertThat(entity.isActive()).isTrue();
		assertThat(entity.getName()).isEqualTo("Pokémon");
		verify(gameRepository).save(entity);
	}

	@Test
	void update_nameUsedByAnotherGame_throwsConflict() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(activeGame()));
		when(gameRepository.existsByNameIgnoreCaseAndIdNot("Pokémon", GAME_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.update(GAME_ID, request)).isInstanceOf(ConflictException.class);
	}

	@Test
	void getById_notFound_throwsResourceNotFound() {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(GAME_ID)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void removeOrHide_gameHasSeries_hidesInsteadOfDeleting() {
		Game game = activeGame();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(seriesRepository.existsByGameId(GAME_ID)).thenReturn(true);

		boolean deleted = service.removeOrHide(GAME_ID);

		assertThat(deleted).isFalse();
		assertThat(game.isActive()).isFalse();
		verify(gameRepository, never()).delete(any());
	}

	@Test
	void removeOrHide_gameHasNoSeries_deletesPermanently() {
		Game game = activeGame();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(seriesRepository.existsByGameId(GAME_ID)).thenReturn(false);

		boolean deleted = service.removeOrHide(GAME_ID);

		assertThat(deleted).isTrue();
		verify(gameRepository).delete(game);
	}
}