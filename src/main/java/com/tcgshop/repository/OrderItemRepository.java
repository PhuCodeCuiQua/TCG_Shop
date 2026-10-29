package com.tcgshop.repository;

import com.tcgshop.entity.OrderItem;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	@Query("select oi.product.id as productId, sum(oi.quantity) as quantity "
			+ "from OrderItem oi where oi.order.id = :orderId "
			+ "group by oi.product.id order by oi.product.id asc")
	List<OrderItemQuantity> findQuantitiesByOrderId(@Param("orderId") Long orderId);

	@Query("select i.order.id as orderId, i.id as itemId, i.product.id as productId, "
			+ "i.productName as productName, i.productImage as productImage, "
			+ "i.cardCondition as cardCondition, i.quantity as quantity, i.price as price, "
			+ "i.totalPrice as totalPrice from OrderItem i where i.order.id in :orderIds "
			+ "order by i.order.id asc, i.id asc")
	List<OrderItemRow> findRowsByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);
}
