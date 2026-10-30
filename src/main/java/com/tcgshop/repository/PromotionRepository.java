package com.tcgshop.repository;

import com.tcgshop.entity.Promotion;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

	@EntityGraph(attributePaths = {"game", "series"})
	@Query(value = """
			select distinct p from Promotion p
			where p.shop is null
				and (:type is null or p.type = :type)
				and (:status is null or p.status = :status)
			""",
			countQuery = """
					select count(p) from Promotion p
					where p.shop is null
						and (:type is null or p.type = :type)
						and (:status is null or p.status = :status)
					""")
	Page<Promotion> searchPlatformPromotions(
			@Param("type") PromotionType type,
			@Param("status") ActiveStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = {"game", "series"})
	@Query(value = """
			select distinct p from Promotion p
			where p.shop is null
				and (:type is null or p.type = :type)
				and (:status is null or p.status = :status)
				and (
					(p.game is null and p.series is null and p.products is empty)
					or exists (
						select product.id
						from Product product
						join product.shop selectedShop
						left join product.series productSeries
						left join product.card productCard
						left join productCard.series cardSeries
						left join productSeries.game directGame
						left join cardSeries.game cardGame
						where selectedShop.id = :shopId
							and selectedShop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
							and (selectedShop.deleted is null or selectedShop.deleted = false)
							and product.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
							and (product.deleted is null or product.deleted = false)
							and (
								product member of p.products
								or (p.series is not null
									and (productSeries = p.series or cardSeries = p.series))
								or (p.game is not null
									and (directGame = p.game or cardGame = p.game))
							)
					)
				)
			""",
			countQuery = """
					select count(p) from Promotion p
					where p.shop is null
						and (:type is null or p.type = :type)
						and (:status is null or p.status = :status)
						and (
							(p.game is null and p.series is null and p.products is empty)
							or exists (
								select product.id
								from Product product
								join product.shop selectedShop
								left join product.series productSeries
								left join product.card productCard
								left join productCard.series cardSeries
								left join productSeries.game directGame
								left join cardSeries.game cardGame
								where selectedShop.id = :shopId
									and selectedShop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
									and (selectedShop.deleted is null or selectedShop.deleted = false)
									and product.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
									and (product.deleted is null or product.deleted = false)
									and (
										product member of p.products
										or (p.series is not null
											and (productSeries = p.series or cardSeries = p.series))
										or (p.game is not null
											and (directGame = p.game or cardGame = p.game))
									)
							)
						)
					""")
	Page<Promotion> searchPlatformPromotionsForShop(
			@Param("type") PromotionType type,
			@Param("status") ActiveStatus status,
			@Param("shopId") Long shopId,
			Pageable pageable);

	@EntityGraph(attributePaths = {"game", "series", "products"})
	@Query("select distinct p from Promotion p where p.id = :id and p.shop is null")
	Optional<Promotion> findPlatformPromotionById(@Param("id") Long id);

	@Query("""
			select distinct p from Promotion p
			left join fetch p.products products
			left join fetch p.series series
			left join fetch p.game game
			where p.shop is null and p.status = :status
				and p.type = :type and p.startDate <= :now and p.endDate >= :now
			""")
	List<Promotion> findActivePlatformPromotions(
			@Param("status") ActiveStatus status,
			@Param("type") PromotionType type,
			@Param("now") LocalDateTime now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Promotion p set p.status = :inactiveStatus
			where p.status = :activeStatus and p.endDate is not null and p.endDate < :now
			""")
	int deactivateExpiredPromotions(
			@Param("activeStatus") ActiveStatus activeStatus,
			@Param("inactiveStatus") ActiveStatus inactiveStatus,
			@Param("now") LocalDateTime now);
}
