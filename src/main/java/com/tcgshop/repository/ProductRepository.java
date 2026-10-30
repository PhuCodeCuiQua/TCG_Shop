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

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>,
		PublicProductSearchRepositoryCustom {

	// Điều kiện tồn kho được kiểm tra ngay trong UPDATE nguyên tử để hai checkout đồng thời không bán vượt kho.
	// Bulk UPDATE bỏ qua @Version, nên tăng thủ công để vô hiệu hóa các entity đang dùng phiên bản cũ.
	// Không dùng clearAutomatically để tránh detach các entity đang được quản lý trong transaction hiện tại (User, Product).
	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.stock = p.stock - :qty, p.version = p.version + 1 "
			+ "where p.id = :id and p.stock >= :qty and p.status = :status and p.deleted = false")
	int decreaseStock(@Param("id") Long id, @Param("qty") int qty, @Param("status") ProductStatus status);

	// UPDATE cộng trực tiếp trong DB để các giao dịch đồng thời không ghi đè số tồn đọc từ snapshot cũ.
	// Không dùng clearAutomatically: Order và các entity đang quản lý trong giao dịch có thể bị detach,
	// khiến thay đổi tiếp theo trên chúng không được persistence context lưu lại.
	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.stock = p.stock + :qty, p.version = p.version + 1 where p.id = :id")
	int increaseStock(@Param("id") Long id, @Param("qty") int qty);

	// sold_count cập nhật bằng UPDATE nguyên tử thay vì đọc rồi ghi để tránh mất lượt bán khi checkout đồng thời.
	// Không tăng version vì lượt bán không phải chỉnh sửa thông tin sản phẩm; tăng sẽ gây xung đột
	// liên tục cho người bán đang sửa sản phẩm.
	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.soldCount = p.soldCount + :qty where p.id = :id")
	int increaseSoldCount(@Param("id") Long id, @Param("qty") int qty);

	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.soldCount = case when p.soldCount >= :qty "
			+ "then p.soldCount - :qty else 0 end where p.id = :id")
	int decreaseSoldCount(@Param("id") Long id, @Param("qty") int qty);

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

	@Query("""
			select distinct p
			from Product p
			join fetch p.shop shop
			left join fetch p.series directSeries
			left join fetch p.card card
			left join fetch card.series cardSeries
			where p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (:seriesId is null or directSeries.id = :seriesId or cardSeries.id = :seriesId)
			order by p.name
			""")
	List<Product> findAvailableForPromotionOptionsBySeriesId(
			@Param("status") ProductStatus status,
			@Param("seriesId") Long seriesId);

	@Query("""
			select distinct p
			from Product p
			join fetch p.shop shop
			left join fetch p.series directSeries
			left join fetch p.card card
			left join fetch card.series cardSeries
			where p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (:seriesId is null or directSeries.id = :seriesId or cardSeries.id = :seriesId)
				and (:gameId is null or directSeries.game.id = :gameId or cardSeries.game.id = :gameId)
			order by p.name
			""")
	List<Product> findAvailableForPromotionOptionsByGameIdAndSeriesId(
			@Param("status") ProductStatus status,
			@Param("gameId") Long gameId,
			@Param("seriesId") Long seriesId);

	@Query("""
			select distinct p
			from Product p
			join fetch p.shop shop
			left join fetch p.series directSeries
			left join fetch p.card card
			left join fetch card.series cardSeries
			where p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (:seriesId is null or directSeries.id = :seriesId or cardSeries.id = :seriesId)
				and (:gameId is null or directSeries.game.id = :gameId or cardSeries.game.id = :gameId)
				and (:shopId is null or shop.id = :shopId)
			order by p.name
			""")
	List<Product> findAvailableForPromotionOptionsByScope(
			@Param("status") ProductStatus status,
			@Param("gameId") Long gameId,
			@Param("seriesId") Long seriesId,
			@Param("shopId") Long shopId);

	@Query("""
			select distinct p
			from Product p
			join fetch p.shop shop
			where p.id in :productIds
				and p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
			order by p.name
			""")
	List<Product> findAvailableForPromotionOptionsByIdIn(
			@Param("status") ProductStatus status,
			@Param("productIds") Collection<Long> productIds);

	@Query("""
			select distinct p
			from Product p
			join fetch p.shop shop
			left join fetch p.series directSeries
			left join fetch p.card card
			left join fetch card.series cardSeries
			where p.status = :status
				and (p.deleted is null or p.deleted = false)
				and shop.status = com.tcgshop.entity.enums.ShopStatus.APPROVED
				and (shop.deleted is null or shop.deleted = false)
				and (:seriesId is null or directSeries.id = :seriesId or cardSeries.id = :seriesId)
				and (:gameId is null or directSeries.game.id = :gameId or cardSeries.game.id = :gameId)
				and (:shopId is null or shop.id = :shopId)
				and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
			order by p.name
			""")
	List<Product> searchAvailablePromotionOptions(
			@Param("status") ProductStatus status,
			@Param("gameId") Long gameId,
			@Param("seriesId") Long seriesId,
			@Param("shopId") Long shopId,
			@Param("keyword") String keyword,
			Pageable pageable);
}
