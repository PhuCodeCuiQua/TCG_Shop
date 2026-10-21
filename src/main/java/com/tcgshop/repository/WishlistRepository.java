package com.tcgshop.repository;

import com.tcgshop.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	boolean existsByUserIdAndProductId(Long userId, Long productId);

	Page<Wishlist> findByUserId(Long userId, Pageable pageable);
}
