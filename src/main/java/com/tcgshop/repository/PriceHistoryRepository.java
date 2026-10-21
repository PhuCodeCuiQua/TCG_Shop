package com.tcgshop.repository;

import com.tcgshop.entity.PriceHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

	List<PriceHistory> findByCardIdAndRecordedDateBetweenOrderByRecordedDate(
			Long cardId, LocalDate startDate, LocalDate endDate);

	Optional<PriceHistory> findByCardIdAndRecordedDate(Long cardId, LocalDate recordedDate);
}
