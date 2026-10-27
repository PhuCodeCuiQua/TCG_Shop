package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only marketplace product listings for a public series page. */
public interface PublicCatalogProductRepository extends Repository<Product, Long> {

	@Query(value = """
			select distinct p.id as id, shop.id as shopId, shop.name as shopName,
				p.name as name, card.name as cardName, card.cardNumber as cardNumber,
				p.type as type, p.cardCondition as cardCondition, p.price as price,
				p.stock as stock, p.soldCount as soldCount,
				coalesce(nullif(trim(productImage.imageUrl), ''), nullif(trim(card.imageUrl), '')) as imageUrl
			from Product p
			join p.shop shop
			left join p.card card
			left join p.images productImage on productImage.isMain = true
			left join p.series productSeries
			left join productSeries.game productGame
			left join card.series cardSeries
			left join cardSeries.game cardGame
			where (cardSeries.id = :seriesId or productSeries.id = :seriesId)
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and cardSeries.active = true and cardGame.active = true))
				and (productSeries is null or (productSeries.active = true and productGame.active = true))
			order by p.name asc, p.id asc
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
					where (cardSeries.id = :seriesId or productSeries.id = :seriesId)
						and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
						and (p.deleted is null or p.deleted = false)
						and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
						and (shop.deleted is null or shop.deleted = false)
						and (card is null or (card.active = true and cardSeries.active = true and cardGame.active = true))
						and (productSeries is null or (productSeries.active = true and productGame.active = true))
					""")
	Page<PublicCatalogProductProjection> findPublicProductsBySeriesId(
			@Param("seriesId") Long seriesId, Pageable pageable);
}
