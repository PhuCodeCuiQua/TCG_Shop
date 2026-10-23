package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorProductManageRepository extends JpaRepository<Product, Long> {
	@Query("select distinct p from Product p join fetch p.shop s join fetch s.owner left join fetch p.images where p.id = :id and (p.deleted is null or p.deleted = false)")
	Optional<Product> findNotDeletedWithOwnerAndImages(@Param("id") Long id);
}
