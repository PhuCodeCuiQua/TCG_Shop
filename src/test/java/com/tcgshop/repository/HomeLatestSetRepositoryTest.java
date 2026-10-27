package com.tcgshop.repository;

import com.tcgshop.entity.Game;
import com.tcgshop.entity.Series;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class HomeLatestSetRepositoryTest {
    @Autowired private SeriesRepository series;
    @Autowired private GameRepository games;

    @Test
    void latestQueryFiltersHiddenDataOrdersByReleaseDateAndLimitsToFour() {
        Game game = new Game(); game.setName("Latest Game"); game.setCode("LATEST"); game = games.saveAndFlush(game);
        for (int i = 1; i <= 6; i++) save(game, "Set " + i, LocalDate.of(2026, i, 1), true);
        save(game, "Hidden", LocalDate.of(2027, 1, 1), false);
        save(game, "Unknown date", null, true);
        Game hidden = new Game(); hidden.setName("Hidden Game"); hidden.setCode("HIDDEN"); hidden.setActive(false);
        hidden = games.saveAndFlush(hidden);
        save(hidden, "Hidden game set", LocalDate.of(2028, 1, 1), true);
        assertThat(series.findLatestActive(PageRequest.of(0, 4))).extracting(Series::getName)
                .containsExactly("Set 6", "Set 5", "Set 4", "Set 3");
    }

    private void save(Game game, String name, LocalDate date, boolean active) {
        Series set = new Series(); set.setGame(game); set.setName(name); set.setCode(name);
        set.setReleaseDate(date); set.setActive(active); series.saveAndFlush(set);
    }
}
