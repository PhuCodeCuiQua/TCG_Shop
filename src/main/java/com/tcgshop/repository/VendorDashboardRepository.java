package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface VendorDashboardRepository extends Repository<Product, Long> {
	@Query("select count(p) from Product p where p.shop.id = :shopId and p.status = :status "
			+ "and (p.deleted is null or p.deleted = false)")
	long countProductsByShopAndStatus(@Param("shopId") Long shopId, @Param("status") ProductStatus status);

	@Query("select count(o) from Order o where o.shop.id = :shopId and o.status = :status")
	long countOrdersByShopAndStatus(@Param("shopId") Long shopId, @Param("status") OrderStatus status);

	@Query("select sum(o.shopRevenue) from Order o "
			+ "where o.shop.id = :shopId and o.status = :status "
			+ "and o.deliveredAt >= :fromDate and o.deliveredAt < :toDateExclusive")
	BigDecimal sumDeliveredRevenueByShopAndPeriod(
			@Param("shopId") Long shopId,
			@Param("status") OrderStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);
}
