package com.tcgshop.repository;

import com.tcgshop.entity.PriceHistory;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

	List<PriceHistory> findByCardIdAndRecordedDateBetweenOrderByRecordedDate(
			Long cardId, LocalDate startDate, LocalDate endDate);

	Optional<PriceHistory> findByCardIdAndRecordedDate(Long cardId, LocalDate recordedDate);

	@Query("select h from PriceHistory h join fetch h.card c "
			+ "where h.recordedDate = :recordedDate and c.id in :cardIds")
	List<PriceHistory> findForCardsByRecordedDate(
			@Param("recordedDate") LocalDate recordedDate,
			@Param("cardIds") Collection<Long> cardIds);

	@EntityGraph(attributePaths = "card")
	@Query(value = """
			select h from PriceHistory h
			where h.card.id = :cardId
				and (:startDate is null or h.recordedDate >= :startDate)
				and (:endDate is null or h.recordedDate <= :endDate)
			""",
			countQuery = """
					select count(h) from PriceHistory h
					where h.card.id = :cardId
						and (:startDate is null or h.recordedDate >= :startDate)
						and (:endDate is null or h.recordedDate <= :endDate)
					""")
	Page<PriceHistory> searchByCardAndDateRange(
			@Param("cardId") Long cardId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			Pageable pageable);

	Optional<PriceHistory> findTopByCardIdOrderByRecordedDateDesc(Long cardId);

	@Query("""
			select h.card.id as cardId, h.marketPrice as marketPrice
			from PriceHistory h
			where h.source = 'ADMIN'
				and h.card.id in :cardIds
				and h.updatedAt = (
					select max(latest.updatedAt) from PriceHistory latest
					where latest.card.id = h.card.id and latest.source = 'ADMIN'
				)
				and h.id = (
					select max(latest.id) from PriceHistory latest
					where latest.card.id = h.card.id
						and latest.source = 'ADMIN'
						and latest.updatedAt = h.updatedAt
				)
			""")
	List<AdminMarketPrice> findLatestAdminPrices(@Param("cardIds") Collection<Long> cardIds);
}
