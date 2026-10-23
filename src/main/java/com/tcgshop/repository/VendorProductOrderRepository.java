package com.tcgshop.repository;

import com.tcgshop.entity.OrderItem;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorProductOrderRepository extends JpaRepository<OrderItem, Long> {
	@Query("select count(item) from OrderItem item where item.product.id = :productId and item.order.status in :statuses")
	long countForProductInStatuses(@Param("productId") Long productId,
			@Param("statuses") Collection<OrderStatus> statuses);
}
