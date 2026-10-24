package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only product queries for the public homepage. */
public interface HomeProductRepository extends Repository<Product, Long> {

	@Query("""
			select distinct p.id as id, shop.id as shopId, p.name as name,
				coalesce(cardSeries.name, productSeries.name) as seriesName,
				coalesce(cardGame.name, productGame.name) as gameName,
				p.price as price, productImage.imageUrl as imageUrl, p.soldCount as soldCount,
				p.createdAt as viewedAt, p.avgRating as avgRating, p.ratingCount as ratingCount
			from Product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			left join p.images productImage on productImage.isMain = true
			where p.soldCount > 10
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by p.soldCount desc, p.id desc
			""")
	List<HomeProductProjection> findBestSellers(Pageable pageable);

	@Query("""
			select distinct p.id as id, shop.id as shopId, p.name as name,
				coalesce(cardSeries.name, productSeries.name) as seriesName,
				coalesce(cardGame.name, productGame.name) as gameName,
				p.price as price, productImage.imageUrl as imageUrl, p.soldCount as soldCount,
				viewed.viewedAt as viewedAt, p.avgRating as avgRating, p.ratingCount as ratingCount
			from ViewedProduct viewed
			join viewed.product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			left join p.images productImage on productImage.isMain = true
			where viewed.user.id = :userId
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by viewedAt desc
			""")
	List<HomeProductProjection> findRecentlyViewedByUserId(
			@Param("userId") Long userId, Pageable pageable);

	@Query("""
			select distinct p.id as id, shop.id as shopId, p.name as name,
				coalesce(cardSeries.name, productSeries.name) as seriesName,
				coalesce(cardGame.name, productGame.name) as gameName,
				p.price as price, productImage.imageUrl as imageUrl, p.soldCount as soldCount,
				p.createdAt as viewedAt, p.avgRating as avgRating, p.ratingCount as ratingCount
			from Product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			left join p.images productImage on productImage.isMain = true
			where p.soldCount > 10
				and p.ratingCount > 0
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by p.avgRating desc, p.ratingCount desc, p.soldCount desc, p.id desc
			""")
	List<HomeProductProjection> findRecommendedProducts(Pageable pageable);
}
