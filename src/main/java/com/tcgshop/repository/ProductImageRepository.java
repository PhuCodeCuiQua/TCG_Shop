package com.tcgshop.repository;

import com.tcgshop.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	@Query(value = """
			select ranked.seriesId as seriesId, ranked.imageUrl as imageUrl from (
				select c.series_id as seriesId,
					coalesce((
						select pi.image_url
						from products p
						join shops sh on sh.id = p.shop_id
						join product_images pi on pi.product_id = p.id
						where p.card_id = c.id and p.type = 'SINGLE' and p.status = 'ACTIVE'
							and (p.deleted is null or p.deleted = false)
							and sh.status = 'APPROVED' and (sh.deleted is null or sh.deleted = false)
							and pi.image_url is not null and trim(pi.image_url) <> ''
						order by case when pi.is_main = true then 0 else 1 end,
							pi.display_order asc, pi.id asc, p.id asc limit 1
					), nullif(trim(c.image_url), '')) as imageUrl,
					row_number() over (partition by c.series_id order by c.market_price desc, c.id asc) as position
				from cards c
				where c.series_id in :seriesIds
					and (nullif(trim(c.image_url), '') is not null or exists (
						select 1 from products p
						join shops sh on sh.id = p.shop_id
						join product_images pi on pi.product_id = p.id
						where p.card_id = c.id and p.type = 'SINGLE' and p.status = 'ACTIVE'
							and (p.deleted is null or p.deleted = false)
							and sh.status = 'APPROVED' and (sh.deleted is null or sh.deleted = false)
							and pi.image_url is not null and trim(pi.image_url) <> ''
					))
			) ranked where ranked.position = 1
			""", nativeQuery = true)
	List<SeriesCoverProjection> findSeriesCovers(@Param("seriesIds") Collection<Long> seriesIds);

	@Query(value = """
			select p.id as productId, coalesce(
				(select pi.image_url from product_images pi
				 where pi.product_id = p.id and pi.image_url is not null and trim(pi.image_url) <> ''
				 order by case when pi.is_main = true then 0 else 1 end, pi.display_order asc, pi.id asc limit 1),
				nullif(trim(c.image_url), '')) as imageUrl
			from products p left join cards c on c.id = p.card_id where p.id in :productIds
			""", nativeQuery = true)
	List<ProductThumbnailProjection> findThumbnails(@Param("productIds") Collection<Long> productIds);

	@Query(value = """
			select p.id as productId,
				(select nullif(trim(series_card.image_url), '') from cards series_card
				 where series_card.series_id = coalesce(p.series_id, c.series_id)
				 and series_card.image_url is not null and trim(series_card.image_url) <> ''
				 order by series_card.market_price desc, series_card.id asc limit 1) as imageUrl
			from products p left join cards c on c.id = p.card_id
			where p.id in :productIds
			""", nativeQuery = true)
	List<ProductThumbnailProjection> findSeriesFallbacks(@Param("productIds") Collection<Long> productIds);

	@Query("select image from ProductImage image where image.product.id in :productIds and image.isMain = true order by image.id asc")
	List<ProductImage> findMainImages(@Param("productIds") Collection<Long> productIds);

	@Query("""
			select image from ProductImage image
			where image.product.id in :productIds
			and (image.isMain = true or image.isMain is null)
			and not exists (
				select preferred.id from ProductImage preferred
				where preferred.product.id = image.product.id and preferred.isMain = true
			)
			order by image.product.id, image.displayOrder asc, image.id asc
			""")
	List<ProductImage> findPrimaryOrFirstImages(@Param("productIds") Collection<Long> productIds);
}
