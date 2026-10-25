package com.tcgshop.repository;

import com.tcgshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/** Public, paginated review listing; reviewer details are fetched in one query. */
public interface PublicReviewRepository extends Repository<Review, Long> {
	@EntityGraph(attributePaths = "user")
	Page<Review> findByProductIdAndDeletedFalseOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);
}
