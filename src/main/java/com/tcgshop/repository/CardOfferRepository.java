package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn chỉ đọc cho bảng so sánh giá giữa các Vendor (PA-35). */
public interface CardOfferRepository extends Repository<Product, Long> {

	@Query("select p from Product p join fetch p.shop s "
			+ "where p.card.id = :cardId "
			+ "and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE "
			+ "and (p.deleted is null or p.deleted = false) "
			+ "and s.status = com.tcgshop.entity.enums.ShopStatus.APPROVED "
			+ "and (s.deleted is null or s.deleted = false) "
			+ "and p.stock > 0 "
			+ "order by p.price asc")
	List<Product> findAvailableByCardId(@Param("cardId") Long cardId);
}