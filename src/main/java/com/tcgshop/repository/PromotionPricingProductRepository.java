package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PromotionPricingProductRepository extends Repository<Product, Long> {
	@Query("select distinct product from Product product "
			+ "left join fetch product.shop "
			+ "left join fetch product.series sealedSeries left join fetch sealedSeries.game "
			+ "left join fetch product.card card left join fetch card.series cardSeries left join fetch cardSeries.game "
			+ "where product.id in :productIds")
	List<Product> findAllForPromotionPricing(@Param("productIds") Collection<Long> productIds);
}
