package com.tcgshop.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only public single-card listings for chatbot budget searches. */
public interface ChatbotPriceSearchRepository extends Repository<com.tcgshop.entity.Product, Long> {
	@Query(value = """
			select p.id as productId, c.name as cardName,
				coalesce(
					(select nullif(trim(pi.image_url), '') from product_images pi
					 where pi.product_id = p.id and pi.image_url is not null and trim(pi.image_url) <> ''
					 order by case when pi.is_main = true then 0 else 1 end,
						pi.display_order asc, pi.id asc limit 1),
					nullif(trim(c.image_url), '')) as imageUrl,
				sh.name as shopName, p.price as price
			from products p
			join shops sh on sh.id = p.shop_id
			join cards c on c.id = p.card_id
			join series s on s.id = c.series_id
			join games g on g.id = s.game_id
			where p.type = 'SINGLE' and p.status = 'ACTIVE'
				and (p.deleted is null or p.deleted = false) and p.stock > 0
				and sh.status = 'APPROVED' and (sh.deleted is null or sh.deleted = false)
				and c.active = true and s.active = true and g.active = true
				and p.price between :minPrice and :maxPrice
			order by abs(p.price - :targetPrice), p.id
			""", nativeQuery = true)
	List<ChatbotPriceCandidateProjection> findPublicOffersInPriceRange(
			@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice,
			@Param("targetPrice") BigDecimal targetPrice,
			Pageable pageable);
}
