package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Narrow repository for serializing Wishlist counter changes on the shared Product row. */
public interface WishlistProductRepository extends JpaRepository<Product, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :productId")
	Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
