package com.tcgshop.repository;

import com.tcgshop.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	@Query("select oi.product.id as productId, sum(oi.quantity) as quantity "
			+ "from OrderItem oi where oi.order.id = :orderId "
			+ "group by oi.product.id order by oi.product.id asc")
	List<OrderItemQuantity> findQuantitiesByOrderId(@Param("orderId") Long orderId);
}
