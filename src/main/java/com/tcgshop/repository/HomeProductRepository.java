package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only product queries for the public homepage. */
public interface HomeProductRepository extends Repository<Product, Long> {
	@Query(value = """
			select coalesce(
				(select pi.image_url
				 from product_images pi
				 join products p on p.id = pi.product_id
				 join shops s on s.id = p.shop_id
				 where p.card_id = c.id and p.type = 'SINGLE' and p.status = 'ACTIVE'
				   and (p.deleted is null or p.deleted = false) and s.status = 'APPROVED'
				   and (s.deleted is null or s.deleted = false)
				   and pi.image_url is not null and trim(pi.image_url) <> ''
				   and lower(pi.image_url) not like '%placeholder%'
				   and lower(pi.image_url) not like '%home-card-%'
				 order by case when pi.is_main = true then 0 else 1 end,
				          pi.display_order asc, p.price desc, pi.id asc limit 1),
				case when lower(trim(c.image_url)) not like '%placeholder%'
				       and lower(trim(c.image_url)) not like '%home-card-%'
				     then nullif(trim(c.image_url), '') end) as imageUrl
			from cards c
			where c.active = true and c.market_price is not null
			  and exists (
				select 1 from products p join shops s on s.id = p.shop_id
				where p.card_id = c.id and p.type = 'SINGLE' and p.status = 'ACTIVE'
				  and (p.deleted is null or p.deleted = false) and s.status = 'APPROVED'
				  and (s.deleted is null or s.deleted = false))
			  and ((nullif(trim(c.image_url), '') is not null
			        and lower(trim(c.image_url)) not like '%placeholder%'
			        and lower(trim(c.image_url)) not like '%home-card-%') or exists (
				select 1 from products p join shops s on s.id = p.shop_id
				join product_images pi on pi.product_id = p.id
				where p.card_id = c.id and p.type = 'SINGLE' and p.status = 'ACTIVE'
				  and (p.deleted is null or p.deleted = false) and s.status = 'APPROVED'
				  and (s.deleted is null or s.deleted = false)
				  and pi.image_url is not null and trim(pi.image_url) <> ''
				  and lower(pi.image_url) not like '%placeholder%'
				  and lower(pi.image_url) not like '%home-card-%'))
			order by c.market_price desc, c.id desc
			limit :limit
			""", nativeQuery = true)
	List<HeroImageProjection> findHeroImages(@Param("limit") int limit);

	@Query(value = """
			select ranked.seriesId as seriesId, ranked.imageUrl as imageUrl from (
			 select c.series_id as seriesId, coalesce(
			   (select pi.image_url from product_images pi where pi.product_id = p.id
			    and pi.image_url is not null and trim(pi.image_url) <> ''
			    order by case when pi.is_main = true then 0 else 1 end, pi.display_order asc, pi.id asc limit 1),
			   nullif(trim(c.image_url), '')) as imageUrl,
			   row_number() over (partition by c.series_id order by c.market_price desc, p.price desc, p.id desc) as position
			 from products p join cards c on c.id = p.card_id join shops s on s.id = p.shop_id
			 where c.series_id in :seriesIds and p.type = 'SINGLE' and p.status = 'ACTIVE'
			   and (p.deleted is null or p.deleted = false) and s.status = 'APPROVED'
			   and (s.deleted is null or s.deleted = false) and c.active = true and p.price is not null
			) ranked where ranked.position = 1
			""", nativeQuery = true)
	List<SeriesCoverProjection> findSeriesCovers(@Param("seriesIds") java.util.Collection<Long> seriesIds);

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
				p.createdAt as viewedAt, p.avgRating as avgRating, p.ratingCount as ratingCount
			from Product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			left join p.images productImage on productImage.isMain = true
			where p.type in (com.tcgshop.entity.enums.ProductType.SUPPLY,
                    com.tcgshop.entity.enums.ProductType.BOOSTER_BOX,
                    com.tcgshop.entity.enums.ProductType.BOOSTER_PACK,
                    com.tcgshop.entity.enums.ProductType.BUNDLE)
                and p.stock > 0
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by p.createdAt desc, p.id desc
			""")
	List<HomeProductProjection> findSuppliesAndSealed(Pageable pageable);

	@Query("""
			select distinct p.id as id, shop.id as shopId, p.name as name,
				coalesce(cardSeries.name, productSeries.name) as seriesName,
				coalesce(cardGame.name, productGame.name) as gameName,
				p.price as price, coalesce(card.imageUrl, productImage.imageUrl) as imageUrl, p.soldCount as soldCount,
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
