package com.tcgshop.repository;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.enums.CardLanguage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

	// Dùng để quyết định ẩn hay xóa Series (PA-10)
	boolean existsBySeriesId(Long seriesId);

	// Chặn trùng theo ràng buộc unique (series + số thẻ + ngôn ngữ) (PA-11)
	boolean existsBySeriesIdAndCardNumberIgnoreCaseAndLanguage(Long seriesId, String cardNumber,
			CardLanguage language);

	boolean existsBySeriesIdAndCardNumberIgnoreCaseAndLanguageAndIdNot(Long seriesId, String cardNumber,
			CardLanguage language, Long id);

	@EntityGraph(attributePaths = "series")
	List<Card> findByActiveTrueOrderByNameAsc();

	@EntityGraph(attributePaths = {"series", "series.game"})
	Optional<Card> findByIdAndActiveTrue(Long id);

	@Query("select c from Card c join fetch c.series s join fetch s.game g "
			+ "where c.active = true and s.active = true and g.active = true "
			+ "and (lower(c.name) like lower(concat('%', :keyword, '%')) "
			+ "or lower(c.cardNumber) like lower(concat('%', :keyword, '%'))) "
			+ "order by c.name asc, c.cardNumber asc")
	List<Card> findPublicSuggestions(@Param("keyword") String keyword, Pageable pageable);

	// Nạp sẵn series và game khi tìm kiếm phân trang để tránh N+1 khi hiển thị tên series, game
	@Override
	@EntityGraph(attributePaths = {"series", "series.game"})
	Page<Card> findAll(Specification<Card> spec, Pageable pageable);
}