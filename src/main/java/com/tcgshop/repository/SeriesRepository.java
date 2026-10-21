package com.tcgshop.repository;

import com.tcgshop.entity.Series;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesRepository extends JpaRepository<Series, Long> {

	List<Series> findByGameId(Long gameId);
}
