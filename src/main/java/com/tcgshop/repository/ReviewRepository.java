package com.tcgshop.repository;

import com.tcgshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByOrderIdAndProductId(Long orderId, Long productId);

	Page<Review> findByProductId(Long productId, Pageable pageable);
}
