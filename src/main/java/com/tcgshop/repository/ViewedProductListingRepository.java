package com.tcgshop.repository;

import com.tcgshop.entity.ViewedProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read model for the current user's public and still-visible view history. */
public interface ViewedProductListingRepository extends Repository<ViewedProduct, Long> {

	@Query(value = """
			select viewed.id as viewedId, p.id as productId, shop.id as shopId, shop.name as shopName,
				p.name as name, coalesce(cardSeries.name, productSeries.name) as seriesName,
				coalesce(cardGame.name, productGame.name) as gameName, p.price as price,
				coalesce(card.imageUrl, productImage.imageUrl) as imageUrl,
				p.soldCount as soldCount, viewed.viewedAt as viewedAt
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
				and p.status in (com.tcgshop.entity.enums.ProductStatus.ACTIVE,
					com.tcgshop.entity.enums.ProductStatus.OUT_OF_STOCK)
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and cardSeries.active = true and cardGame.active = true))
				and (productSeries is null or (productSeries.active = true and productGame.active = true))
			order by viewed.viewedAt desc, viewed.id desc
			""",
		countQuery = """
			select count(distinct viewed.id)
			from ViewedProduct viewed
			join viewed.product p
			join p.shop shop
			left join p.card card
			left join card.series cardSeries
			left join cardSeries.game cardGame
			left join p.series productSeries
			left join productSeries.game productGame
			where viewed.user.id = :userId
				and p.status in (com.tcgshop.entity.enums.ProductStatus.ACTIVE,
					com.tcgshop.entity.enums.ProductStatus.OUT_OF_STOCK)
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and cardSeries.active = true and cardGame.active = true))
				and (productSeries is null or (productSeries.active = true and productGame.active = true))
			""")
	Page<ViewedProductProjection> findVisibleHistoryByUserId(
			@Param("userId") Long userId, Pageable pageable);
}
