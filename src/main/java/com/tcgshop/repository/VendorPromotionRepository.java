package com.tcgshop.repository;

import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorPromotionRepository extends JpaRepository<Promotion, Long> {
	Page<Promotion> findAllByShopId(Long shopId, Pageable pageable);

	@Query("select distinct p from Promotion p join fetch p.shop s join fetch s.owner left join fetch p.products where p.id = :id and p.shop is not null")
	Optional<Promotion> findByIdWithShopOwnerAndProducts(@Param("id") Long id);

	@Query("select count(distinct promotion) from Promotion promotion join promotion.products target "
			+ "where promotion.shop.id = :shopId and promotion.type = :type and promotion.status = :status "
			+ "and (:excludedId is null or promotion.id <> :excludedId) "
			+ "and promotion.startDate <= :endDate and promotion.endDate >= :startDate "
			+ "and target.id in :productIds")
	long countOverlappingActivePromotions(
			@Param("shopId") Long shopId,
			@Param("type") PromotionType type,
			@Param("status") ActiveStatus status,
			@Param("excludedId") Long excludedId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate,
			@Param("productIds") Collection<Long> productIds);

	@Query("select distinct promotion from Promotion promotion join fetch promotion.products target "
			+ "where promotion.shop is not null and promotion.type = :type and promotion.status = :status "
			+ "and promotion.startDate <= :now and promotion.endDate >= :now "
			+ "and target.id in :productIds")
	List<Promotion> findActiveShopPromotionsForProducts(
			@Param("type") PromotionType type,
			@Param("status") ActiveStatus status,
			@Param("now") LocalDateTime now,
			@Param("productIds") Collection<Long> productIds);
}
