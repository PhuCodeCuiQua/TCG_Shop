package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorSingleProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			select case when count(product) > 0 then true else false end
			from Product product
			where product.shop.id = :shopId
				and product.card.id = :cardId
				and product.cardCondition = :condition
				and product.language = :language
				and ((:edition is null and product.edition is null) or product.edition = :edition)
				and (product.deleted is null or product.deleted = false)
			""")
	boolean existsActiveListingVariant(
			@Param("shopId") Long shopId,
			@Param("cardId") Long cardId,
			@Param("condition") CardCondition condition,
			@Param("language") CardLanguage language,
			@Param("edition") String edition);
}
