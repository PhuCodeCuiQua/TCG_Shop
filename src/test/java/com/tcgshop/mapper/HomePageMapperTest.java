package com.tcgshop.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Game;
import com.tcgshop.repository.HomeProductProjection;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class HomePageMapperTest {
	private final HomePageMapper mapper = Mappers.getMapper(HomePageMapper.class);

	@Test
	void productStringsAndImageUrlArePreservedInFull() {
		HomeProductProjection projection = mock(HomeProductProjection.class);
		when(projection.getId()).thenReturn(15L);
		when(projection.getName()).thenReturn("Pikachu Collector Edition");
		when(projection.getSeriesName()).thenReturn("Prismatic Horizons");
		when(projection.getGameName()).thenReturn("Pokémon");
		when(projection.getImageUrl()).thenReturn("https://example.com/cards/pikachu.png");
		when(projection.getPrice()).thenReturn(new BigDecimal("125000"));

		var response = mapper.toProductResponse(projection);
		assertThat(response.id()).isEqualTo(15L);
		assertThat(response.name()).isEqualTo("Pikachu Collector Edition");
		assertThat(response.seriesName()).isEqualTo("Prismatic Horizons");
		assertThat(response.gameName()).isEqualTo("Pokémon");
		assertThat(response.imageUrl()).isEqualTo("https://example.com/cards/pikachu.png");
		assertThat(response.price()).isEqualByComparingTo("125000");
	}

	@Test
	void gameInitialDoesNotTruncateNameOrLogoUrl() {
		Game game = new Game();
		game.setName("Magic the Gathering");
		game.setLogoUrl("https://example.com/games/magic.png");
		var response = mapper.toGameResponse(game);
		assertThat(response.mark()).isEqualTo("M");
		assertThat(response.name()).isEqualTo("Magic the Gathering");
		assertThat(response.logoUrl()).isEqualTo("https://example.com/games/magic.png");
	}

	@Test
	void missingImageStaysNullForTemplateFallback() {
		HomeProductProjection projection = mock(HomeProductProjection.class);
		assertThat(mapper.toProductResponse(projection).imageUrl()).isNull();
	}
}
