package com.tcgshop.repository;

import com.tcgshop.entity.Wishlist;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	boolean existsByUserIdAndProductId(Long userId, Long productId);

	Page<Wishlist> findByUserId(Long userId, Pageable pageable);

	Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

	long countByProductId(Long productId);

	@Query("""
			select w.product.id from Wishlist w
			join w.product p
			join p.shop shop
			left join p.card card
			where w.user.id = :userId and p.id in :productIds
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and card.series.active = true and card.series.game.active = true))
			""")
	List<Long> findProductIdsByUserIdAndProductIdIn(
			@Param("userId") Long userId, @Param("productIds") Collection<Long> productIds);

	@Query(value = """
			select w.id as wishlistId, p.id as productId, p.name as name, shop.name as shopName,
				p.price as price, coalesce(card.imageUrl, image.imageUrl) as imageUrl,
				p.favoriteCount as favoriteCount, p.soldCount as soldCount
			from Wishlist w
			join w.product p
			join p.shop shop
			left join p.card card
			left join p.images image on image.isMain = true
			where w.user.id = :userId
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and card.series.active = true and card.series.game.active = true))
				and (p.series is null or (p.series.active = true and p.series.game.active = true))
			order by w.createdAt desc, p.id desc
			""",
		countQuery = """
			select count(w)
			from Wishlist w
			join w.product p
			join p.shop shop
			left join p.card card
			where w.user.id = :userId
				and p.status = com.tcgshop.entity.enums.ProductStatus.ACTIVE
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (card is null or (card.active = true and card.series.active = true and card.series.game.active = true))
				and (p.series is null or (p.series.active = true and p.series.game.active = true))
			""")
	Page<WishlistProductProjection> findVisibleProductsByUserId(
			@Param("userId") Long userId, Pageable pageable);
}
