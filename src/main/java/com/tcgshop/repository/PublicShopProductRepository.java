package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
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
			left join p.images productImage on productImage.isMain = true
			where shop.id = :shopId
				and shop.status = :shopStatus
				and (shop.deleted is null or shop.deleted = false)
				and p.status = :productStatus
				and (p.deleted is null or p.deleted = false)
				and (:productType is null or p.type = :productType)
			""",
		countQuery = """
			select count(distinct p.id)
			from Product p
			join p.shop shop
			where shop.id = :shopId
				and shop.status = :shopStatus
				and (shop.deleted is null or shop.deleted = false)
				and p.status = :productStatus
				and (p.deleted is null or p.deleted = false)
				and (:productType is null or p.type = :productType)
			""")
	Page<PublicShopProductProjection> findPublicProducts(
			@Param("shopId") Long shopId,
			@Param("shopStatus") ShopStatus shopStatus,
			@Param("productStatus") ProductStatus productStatus,
			@Param("productType") ProductType productType,
			Pageable pageable);
}
