package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn thống kê chỉ đọc cho dashboard (PA-22). */
public interface DashboardRepository extends Repository<Order, Long> {

	interface DeliveredRow {
		LocalDateTime getDeliveredAt();
		BigDecimal getTotalAmount();
		BigDecimal getCommissionAmount();
	}

	interface StatusRow {
		OrderStatus getStatus();
		Long getTotal();
	}

	interface ShopRow {
		Long getShopId();
		String getShopName();
		BigDecimal getRevenue();
		Long getOrderCount();
	}

	interface ProductRow {
		Long getProductId();
		String getProductName();
		Long getQuantity();
	}

	@Query("select o.deliveredAt as deliveredAt, o.totalAmount as totalAmount, "
			+ "o.commissionAmount as commissionAmount from com.tcgshop.entity.Order o "
			+ "where o.status = :status and o.deliveredAt >= :from and o.deliveredAt < :to")
	List<DeliveredRow> findDeliveredRows(@Param("status") OrderStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("select count(o) from com.tcgshop.entity.Order o where o.createdAt >= :from and o.createdAt < :to")
	long countCreated(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("select o.status as status, count(o) as total from com.tcgshop.entity.Order o "
			+ "where o.createdAt >= :from and o.createdAt < :to group by o.status")
	List<StatusRow> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("select s.id as shopId, s.name as shopName, sum(o.totalAmount) as revenue, count(o) as orderCount "
			+ "from com.tcgshop.entity.Order o join o.shop s "
			+ "where o.status = :status and o.deliveredAt >= :from and o.deliveredAt < :to "
			+ "group by s.id, s.name order by sum(o.totalAmount) desc")
	List<ShopRow> findTopShops(@Param("status") OrderStatus status, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to, Pageable pageable);

	@Query("select p.id as productId, p.name as productName, sum(oi.quantity) as quantity "
			+ "from com.tcgshop.entity.OrderItem oi join oi.order o join oi.product p "
			+ "where o.status = :status and o.deliveredAt >= :from and o.deliveredAt < :to "
			+ "group by p.id, p.name order by sum(oi.quantity) desc")
	List<ProductRow> findTopProducts(@Param("status") OrderStatus status, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to, Pageable pageable);
}