package com.tcgshop.repository;

import com.tcgshop.entity.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Aggregates visible review ratings without loading review entities. */
public interface ReviewAggregateRepository extends Repository<Review, Long> {
	@Query("select avg(review.rating) as averageRating, count(review.id) as reviewCount "
			+ "from Review review where review.product.id = :productId and review.deleted = false")
	Optional<ReviewRatingAggregate> summarizeByProductId(@Param("productId") Long productId);
}
