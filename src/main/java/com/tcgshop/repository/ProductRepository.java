package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// Điều kiện tồn kho được kiểm tra ngay trong UPDATE nguyên tử để hai checkout đồng thời không bán vượt kho.
	// Bulk UPDATE bỏ qua @Version, nên tăng thủ công để vô hiệu hóa các entity đang dùng phiên bản cũ.
	// clearAutomatically loại entity cũ khỏi persistence context sau UPDATE, tránh ghi đè lại tồn kho vừa trừ.
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Product p set p.stock = p.stock - :qty, p.version = p.version + 1 "
			+ "where p.id = :id and p.stock >= :qty and p.status = :status and p.deleted = false")
	int decreaseStock(@Param("id") Long id, @Param("qty") int qty, @Param("status") ProductStatus status);

	@Query("select p from Product p join fetch p.shop shop join fetch shop.owner where p.id in :ids")
	List<Product> findAllWithShopOwnerByIdIn(@Param("ids") Collection<Long> ids);

	// Dùng để quyết định ẩn hay xóa Series: hàng niêm phong gắn trực tiếp với series (PA-10)
	boolean existsBySeriesId(Long seriesId);

	@Query(value = """
			select p
			from Product p
			left join fetch p.shop s
			where (p.deleted is null or p.deleted = false)
				and (:shopId is null or s.id = :shopId)
				and (:type is null or p.type = :type)
				and (:status is null or p.status = :status)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
					or lower(s.name) like lower(concat('%', :keyword, '%')))
			""",
			countQuery = """
					select count(p)
					from Product p
					left join p.shop s
					where (p.deleted is null or p.deleted = false)
						and (:shopId is null or s.id = :shopId)
						and (:type is null or p.type = :type)
						and (:status is null or p.status = :status)
						and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
							or lower(s.name) like lower(concat('%', :keyword, '%')))
					""")
	Page<Product> searchForModeration(
			@Param("shopId") Long shopId,
			@Param("type") ProductType type,
			@Param("status") ProductStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			select p
			from Product p
			left join fetch p.shop s
			left join fetch s.owner
			where p.id = :productId
				and (p.deleted is null or p.deleted = false)
			""")
	Optional<Product> findNotDeletedByIdWithShopOwner(@Param("productId") Long productId);

	@Query("""
			select p from Product p
			join fetch p.shop shop
			where p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by p.name
			""")
	java.util.List<Product> findAvailableForPromotionOptions(
			@Param("status") ProductStatus status);
}
