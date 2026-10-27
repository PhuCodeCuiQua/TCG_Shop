package com.tcgshop.repository;

import com.tcgshop.entity.Series;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesRepository extends JpaRepository<Series, Long> {
	@EntityGraph(attributePaths = "game")
	@Query("select s from Series s where s.active = true and s.game.active = true "
			+ "and upper(s.game.code) = 'YGO' and upper(s.code) in ('LOB', 'MRD', 'SRL') "
			+ "order by case upper(s.code) when 'LOB' then 1 when 'MRD' then 2 else 3 end")
	List<Series> findFeaturedYuGiOhSets();

	@EntityGraph(attributePaths = "game")
	@Query("select s from Series s where s.active = true and s.game.active = true "
			+ "order by case when s.releaseDate is null then 1 else 0 end, s.releaseDate desc, s.createdAt desc, s.id desc")
	List<Series> findLatestActive(Pageable pageable);

	List<Series> findByGameId(Long gameId);

	@EntityGraph(attributePaths = "game")
	@Query("select s from Series s where s.active = true and s.game.active = true order by s.game.name, s.name")
	List<Series> findActivePromotionOptions();

	// Dùng để quyết định ẩn hay xóa Game
	boolean existsByGameId(Long gameId);

	boolean existsByGameIdAndNameIgnoreCase(Long gameId, String name);

	boolean existsByGameIdAndNameIgnoreCaseAndIdNot(Long gameId, String name, Long id);

	boolean existsByGameIdAndCodeIgnoreCase(Long gameId, String code);

	boolean existsByGameIdAndCodeIgnoreCaseAndIdNot(Long gameId, String code, Long id);

	// EntityGraph nạp sẵn game để tránh N+1 khi hiển thị gameName
	@EntityGraph(attributePaths = "game")
	@Query("""
			select s
			from Series s
			where (:gameId is null or s.game.id = :gameId)
				and (:keyword is null
					or lower(s.name) like lower(concat('%', :keyword, '%'))
					or lower(s.code) like lower(concat('%', :keyword, '%')))
				and (:active is null or s.active = :active)
			""")
	Page<Series> search(
			@Param("keyword") String keyword,
			@Param("gameId") Long gameId,
			@Param("active") Boolean active,
			Pageable pageable);
}
