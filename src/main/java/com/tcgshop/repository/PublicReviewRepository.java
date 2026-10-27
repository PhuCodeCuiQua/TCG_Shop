package com.tcgshop.repository;

import com.tcgshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Public, paginated review listing; reviewer details are fetched in one query. */
public interface PublicReviewRepository extends Repository<Review, Long> {
	@EntityGraph(attributePaths = "user")
	@Query("select review from Review review "
			+ "where review.product.id = :productId and review.deleted = false "
			+ "and (:rating is null or review.rating = :rating)")
	Page<Review> findVisibleByProductAndRating(
			@Param("productId") Long productId, @Param("rating") Integer rating, Pageable pageable);
}
