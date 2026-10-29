package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface VendorPromotionProductRepository extends Repository<Product, Long> {
	@Query("select product from Product product where product.shop.id = :shopId "
			+ "and (product.deleted is null or product.deleted = false) order by product.name asc")
	List<Product> findPromotionOptionsByShopId(@Param("shopId") Long shopId);

	@Query("select product from Product product where product.shop.id = :shopId "
			+ "and product.id in :productIds and (product.deleted is null or product.deleted = false)")
	List<Product> findOwnedProductsByIds(
			@Param("shopId") Long shopId,
			@Param("productIds") Collection<Long> productIds);
}
