package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface VendorProductRepository extends Repository<Product, Long> {

	@Query(value = """
			select p.id as id,
				p.name as name,
				p.type as type,
				p.price as price,
				p.stock as stock,
				p.status as status,
				coalesce((select pi.image_url
					from product_images pi
					where pi.product_id = p.id and pi.image_url is not null and trim(pi.image_url) <> ''
					order by case when pi.is_main = true then 0 else 1 end,
						pi.display_order asc, pi.id asc
					limit 1), nullif(trim(c.image_url), '')) as imageUrl,
				p.created_at as createdAt
			from products p
			left join cards c on c.id = p.card_id
			where p.shop_id = :shopId
				and (p.deleted is null or p.deleted = false)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
				and (:type is null or p.type = :type)
				and (:status is null or p.status = :status)
			order by p.created_at desc, p.id desc
			""",
			countQuery = """
			select count(*)
			from products p
			where p.shop_id = :shopId
				and (p.deleted is null or p.deleted = false)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
				and (:type is null or p.type = :type)
				and (:status is null or p.status = :status)
			""",
			nativeQuery = true)
	Page<VendorProductProjection> searchOwnedProducts(
			@Param("shopId") Long shopId,
			@Param("keyword") String keyword,
			@Param("type") String type,
			@Param("status") String status,
			Pageable pageable);
}
