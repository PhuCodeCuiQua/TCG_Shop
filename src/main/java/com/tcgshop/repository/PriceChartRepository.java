package com.tcgshop.repository;

import com.tcgshop.entity.PriceHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn chỉ đọc cho biểu đồ giá (PA-36). */
public interface PriceChartRepository extends Repository<PriceHistory, Long> {

	@Query("select ph from PriceHistory ph where ph.card.id = :cardId "
			+ "and ph.recordedDate between :from and :to and ph.marketPrice is not null "
			+ "order by ph.recordedDate asc")
	List<PriceHistory> findRange(@Param("cardId") Long cardId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);
}