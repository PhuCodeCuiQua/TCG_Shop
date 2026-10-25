package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read-only report queries restricted to one shop and delivered date range. */
public interface VendorRevenueRepository extends JpaRepository<Order, Long> {

	@Query("""
			select sum(coalesce(o.shopRevenue, :zero) + coalesce(o.commissionAmount, :zero)) as revenueBase,
				sum(coalesce(o.commissionAmount, :zero)) as commissionTotal,
				sum(coalesce(o.shopRevenue, :zero)) as receivedTotal,
				count(o) as completedOrders
			from Order o
			where o.shop.id = :shopId
				and o.status = :status
				and (:fromDate is null or o.deliveredAt >= :fromDate)
				and (:toDateExclusive is null or o.deliveredAt < :toDateExclusive)
			""")
	VendorRevenueTotals summarizeDeliveredOrders(
			@Param("shopId") Long shopId,
			@Param("status") OrderStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive,
			@Param("zero") BigDecimal zero);

	@Query(value = """
			select o.id as orderId,
				o.orderCode as orderCode,
				o.deliveredAt as deliveredAt,
				(coalesce(o.shopRevenue, :zero) + coalesce(o.commissionAmount, :zero)) as revenueBase,
				coalesce(o.commissionAmount, :zero) as commissionAmount,
				coalesce(o.shopRevenue, :zero) as shopRevenue
			from Order o
			where o.shop.id = :shopId
				and o.status = :status
				and (:fromDate is null or o.deliveredAt >= :fromDate)
				and (:toDateExclusive is null or o.deliveredAt < :toDateExclusive)
			order by o.deliveredAt desc, o.id desc
			""",
			countQuery = """
			select count(o)
			from Order o
			where o.shop.id = :shopId
				and o.status = :status
				and (:fromDate is null or o.deliveredAt >= :fromDate)
				and (:toDateExclusive is null or o.deliveredAt < :toDateExclusive)
			""")
	Page<VendorRevenueOrderRow> findDeliveredOrders(
			@Param("shopId") Long shopId,
			@Param("status") OrderStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive,
			@Param("zero") BigDecimal zero,
			Pageable pageable);

	@Query(value = """
			select date_format(o.delivered_at, :periodFormat) as period,
				sum(coalesce(o.shop_revenue, 0)) as revenue
			from orders o
			where o.shop_id = :shopId
				and o.status = 'DELIVERED'
				and (:fromDate is null or o.delivered_at >= :fromDate)
				and (:toDateExclusive is null or o.delivered_at < :toDateExclusive)
			group by date_format(o.delivered_at, :periodFormat)
			order by min(o.delivered_at)
			""", nativeQuery = true)
	List<VendorRevenueTrendRow> findDeliveredRevenueTrend(
			@Param("shopId") Long shopId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive,
			@Param("periodFormat") String periodFormat);

	@Query(value = """
			select oi.product_name as productName, sum(oi.quantity) as quantitySold
			from orders o
			join order_items oi on oi.order_id = o.id
			where o.shop_id = :shopId
				and o.status = 'DELIVERED'
				and (:fromDate is null or o.delivered_at >= :fromDate)
				and (:toDateExclusive is null or o.delivered_at < :toDateExclusive)
			group by oi.product_name
			order by quantitySold desc, oi.product_name asc
			""", nativeQuery = true)
	List<VendorTopProductRow> findTopDeliveredProducts(
			@Param("shopId") Long shopId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive,
			Pageable pageable);

	@Query(value = """
			select o.status as status, count(*) as orderCount
			from orders o
			where o.shop_id = :shopId
				and (:fromDate is null or o.created_at >= :fromDate)
				and (:toDateExclusive is null or o.created_at < :toDateExclusive)
			group by o.status
			""", nativeQuery = true)
	List<VendorOrderStatusRow> countOrdersByStatus(
			@Param("shopId") Long shopId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);

	interface VendorRevenueTotals {
		BigDecimal getRevenueBase();

		BigDecimal getCommissionTotal();

		BigDecimal getReceivedTotal();

		Long getCompletedOrders();
	}

	interface VendorRevenueOrderRow {
		Long getOrderId();

		String getOrderCode();

		LocalDateTime getDeliveredAt();

		BigDecimal getRevenueBase();

		BigDecimal getCommissionAmount();

		BigDecimal getShopRevenue();
	}

	interface VendorRevenueTrendRow {
		String getPeriod();

		BigDecimal getRevenue();
	}

	interface VendorTopProductRow {
		String getProductName();

		Long getQuantitySold();
	}

	interface VendorOrderStatusRow {
		String getStatus();

		Long getOrderCount();
	}
}
