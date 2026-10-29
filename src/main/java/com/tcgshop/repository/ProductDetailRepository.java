package com.tcgshop.repository;

import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn chỉ đọc cho trang chi tiết sản phẩm (PA-34). */
public interface ProductDetailRepository extends Repository<Product, Long> {

	@Query("select p from Product p join fetch p.shop s "
			+ "left join fetch p.card c left join fetch c.series cs left join fetch cs.game cg "
			+ "left join fetch p.series ps "
			+ "where p.id = :id")
	Optional<Product> findDetailById(@Param("id") Long id);

	@Query("select p from Product p join fetch p.shop s "
			+ "where p.card.id = :cardId "
			+ "and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE "
			+ "and (p.deleted is null or p.deleted = false) "
			+ "and s.status = com.tcgshop.entity.enums.ShopStatus.APPROVED "
			+ "order by p.price asc")
	List<Product> findOffersByCardId(@Param("cardId") Long cardId);

	@Query("select ph from PriceHistory ph where ph.card.id = :cardId order by ph.recordedDate desc")
	List<PriceHistory> findLatestPrices(@Param("cardId") Long cardId, Pageable pageable);
}