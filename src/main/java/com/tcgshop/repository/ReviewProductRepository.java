package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Product visibility and row-lock queries isolated from the shared ProductRepository. */
public interface ReviewProductRepository extends Repository<Product, Long> {
	@Query("select p from Product p join p.shop shop where p.id = :productId "
			+ "and p.status in (com.tcgshop.entity.enums.ProductStatus.ACTIVE, "
			+ "com.tcgshop.entity.enums.ProductStatus.OUT_OF_STOCK) "
			+ "and (p.deleted is null or p.deleted = false) "
			+ "and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED "
			+ "and (shop.deleted is null or shop.deleted = false)")
	Optional<Product> findPublicProduct(@Param("productId") Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p join p.shop shop where p.id = :productId "
			+ "and p.status in (com.tcgshop.entity.enums.ProductStatus.ACTIVE, "
			+ "com.tcgshop.entity.enums.ProductStatus.OUT_OF_STOCK) "
			+ "and (p.deleted is null or p.deleted = false) "
			+ "and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED "
			+ "and (shop.deleted is null or shop.deleted = false)")
	Optional<Product> findPublicProductForUpdate(@Param("productId") Long productId);
}
