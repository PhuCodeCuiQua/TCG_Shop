package com.tcgshop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only statistics queries scoped to the currently assigned Shipper. */
public interface ShipperStatisticsRepository extends Repository<com.tcgshop.entity.Order, Long> {
	@Query(value = """
			select count(*) from orders o
			where o.shipper_id = :shipperId and o.status in ('CONFIRMED', 'PICKED_UP', 'SHIPPING')
			""", nativeQuery = true)
	long countOrdersToDeliver(@Param("shipperId") Long shipperId);

	@Query(value = """
			select count(*) from orders o
			where o.shipper_id = :shipperId and o.status = 'DELIVERED'
				and o.delivered_at >= :fromDate and o.delivered_at < :toDateExclusive
			""", nativeQuery = true)
	long countDeliveredBetween(@Param("shipperId") Long shipperId,
			@Param("fromDate") LocalDateTime fromDate, @Param("toDateExclusive") LocalDateTime toDateExclusive);

	@Query(value = """
			select coalesce(sum(o.total_amount), 0)
			from orders o join order_groups og on og.id = o.order_group_id
			where o.shipper_id = :shipperId and o.status = 'DELIVERED' and o.cod_collected = true
				and og.payment_method = 'COD'
				and o.delivered_at >= :fromDate and o.delivered_at < :toDateExclusive
			""", nativeQuery = true)
	BigDecimal sumCollectedCodBetween(@Param("shipperId") Long shipperId,
			@Param("fromDate") LocalDateTime fromDate, @Param("toDateExclusive") LocalDateTime toDateExclusive);

	@Query(value = """
			select
				coalesce(sum(case when o.status = 'DELIVERED'
					and (:fromDate is null or o.delivered_at >= :fromDate)
					and (:toDateExclusive is null or o.delivered_at < :toDateExclusive)
					then 1 else 0 end), 0) as deliveredCount,
				coalesce(sum(case when o.status = 'DELIVERY_FAILED'
					and (:fromDate is null or o.updated_at >= :fromDate)
					and (:toDateExclusive is null or o.updated_at < :toDateExclusive)
					then 1 else 0 end), 0) as deliveryFailedCount,
				coalesce(sum(case when o.status in ('PICKED_UP', 'SHIPPING')
					then 1 else 0 end), 0) as inProgressCount,
				coalesce(sum(case when o.status = 'DELIVERED' and o.cod_collected = true
					and og.payment_method = 'COD'
					and (:fromDate is null or o.delivered_at >= :fromDate)
					and (:toDateExclusive is null or o.delivered_at < :toDateExclusive)
					then o.total_amount else 0 end), 0) as codCollectedTotal,
				coalesce(sum(case when o.status = 'DELIVERED'
					and (:fromDate is null or o.delivered_at >= :fromDate)
					and (:toDateExclusive is null or o.delivered_at < :toDateExclusive)
					then coalesce(o.shipping_fee, 0) else 0 end), 0) as totalIncome
			from orders o
			left join order_groups og on og.id = o.order_group_id
			where o.shipper_id = :shipperId
			""", nativeQuery = true)
	ShipperStatisticsTotals summarizeByShipper(
			@Param("shipperId") Long shipperId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);

	@Query(value = """
			select date_format(date(o.delivered_at), '%Y-%m-%d') as deliveryDate, count(*) as orderCount
			from orders o
			where o.shipper_id = :shipperId
				and o.status = 'DELIVERED'
				and o.delivered_at >= :fromDate
				and o.delivered_at < :toDateExclusive
			group by deliveryDate
			order by deliveryDate
			""", nativeQuery = true)
	List<ShipperDeliveryCountRow> findDeliveredCountsByDay(
			@Param("shipperId") Long shipperId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);

	interface ShipperStatisticsTotals {
		Long getDeliveredCount();

		Long getDeliveryFailedCount();

		Long getInProgressCount();

		BigDecimal getCodCollectedTotal();

		BigDecimal getTotalIncome();
	}

	interface ShipperDeliveryCountRow {
		String getDeliveryDate();

		Long getOrderCount();
	}
}
