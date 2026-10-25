package com.tcgshop.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn tổng hợp giá bán snapshot của các đơn đã giao trong cửa sổ PA-39. */
public interface MarketPriceJobRepository extends Repository<com.tcgshop.entity.OrderItem, Long> {

	@Query("""
			select p.card.id as cardId,
				sum(oi.price * oi.quantity) as salesAmount,
				sum(oi.quantity) as quantity
			from OrderItem oi
			join oi.order o
			join oi.product p
			where o.status = com.tcgshop.entity.enums.OrderStatus.DELIVERED
				and o.deliveredAt >= :since
				and o.deliveredAt <= :through
				and oi.price is not null
				and oi.quantity > 0
				and p.card is not null
				and p.type = com.tcgshop.entity.enums.ProductType.SINGLE
			group by p.card.id
			""")
	List<DeliveredCardSales> findDeliveredCardSalesBetween(
			@Param("since") LocalDateTime since,
			@Param("through") LocalDateTime through);
}
