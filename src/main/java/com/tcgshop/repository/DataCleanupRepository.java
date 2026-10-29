package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DataCleanupRepository extends JpaRepository<Product, Long> {
	@Query("""
			select distinct p from Product p
			left join fetch p.images
			left join fetch p.card
			where p.deleted is null or p.deleted = false
			""")
	List<Product> findProductsForImageReview();
}
