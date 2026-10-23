package com.tcgshop.repository;

import com.tcgshop.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

	// Dùng để quyết định ẩn hay xóa Series (PA-10)
	boolean existsBySeriesId(Long seriesId);
}