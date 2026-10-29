package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.ShopStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Isolated read model for public shop listings; leaves the moderation ProductRepository untouched. */
public interface PublicShopProductRepository extends Repository<Product, Long> {

	@Query(value = """
			select distinct p.id as id, p.name as name, p.description as description,
				p.price as price, p.stock as stock, p.type as type, productImage.imageUrl as imageUrl,
				p.createdAt as createdAt
			from Product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			left join p.images productImage on productImage.isMain = true
			where shop.id = :shopId
				and shop.status = :shopStatus
				and (shop.deleted is null or shop.deleted = false)
				and p.status = :productStatus
				and (p.deleted is null or p.deleted = false)
				and (p.type <> :singleType or (
					card.active = true and cardSeries.active = true and cardGame.active = true))
				and (productSeries.id is null or (
					productSeries.active = true and productGame.active = true))
				and (:productType is null or p.type = :productType)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
				and (:gameId is null or cardGame.id = :gameId or productGame.id = :gameId)
				and (:seriesId is null or cardSeries.id = :seriesId or productSeries.id = :seriesId)
				and (:rarity is null or card.rarity = :rarity)
				and (:minPrice is null or p.price >= :minPrice)
				and (:maxPrice is null or p.price <= :maxPrice)
			""",
		countQuery = """
			select count(distinct p.id)
			from Product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			where shop.id = :shopId
				and shop.status = :shopStatus
				and (shop.deleted is null or shop.deleted = false)
				and p.status = :productStatus
				and (p.deleted is null or p.deleted = false)
				and (p.type <> :singleType or (
					card.active = true and cardSeries.active = true and cardGame.active = true))
				and (productSeries.id is null or (
					productSeries.active = true and productGame.active = true))
				and (:productType is null or p.type = :productType)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
				and (:gameId is null or cardGame.id = :gameId or productGame.id = :gameId)
				and (:seriesId is null or cardSeries.id = :seriesId or productSeries.id = :seriesId)
				and (:rarity is null or card.rarity = :rarity)
				and (:minPrice is null or p.price >= :minPrice)
				and (:maxPrice is null or p.price <= :maxPrice)
			""")
	Page<PublicShopProductProjection> findPublicProducts(
			@Param("shopId") Long shopId,
			@Param("shopStatus") ShopStatus shopStatus,
			@Param("productStatus") ProductStatus productStatus,
			@Param("productType") ProductType productType,
			@Param("keyword") String keyword,
			@Param("gameId") Long gameId,
			@Param("seriesId") Long seriesId,
			@Param("rarity") Rarity rarity,
			@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice,
			@Param("singleType") ProductType singleType,
			Pageable pageable);
}
