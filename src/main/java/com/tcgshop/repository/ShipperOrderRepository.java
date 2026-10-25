package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipperOrderRepository extends JpaRepository<Order, Long> {
	@Query(value = """
			select o from Order o
			join fetch o.shop
			left join fetch o.orderGroup
			where o.shipper.id = :shipperId and o.status in :statuses
			""",
			countQuery = """
			select count(o.id) from Order o
			where o.shipper.id = :shipperId and o.status in :statuses
			""")
	Page<Order> findAssignedOrders(@Param("shipperId") Long shipperId,
			@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);

	@Query("""
			select o.status as status, count(o.id) as total
			from Order o
			where o.shipper.id = :shipperId
			group by o.status
			""")
	List<ShipperOrderStatusCount> countStatusesByShipperId(@Param("shipperId") Long shipperId);

	@EntityGraph(attributePaths = {"shop", "orderGroup", "items"})
	Optional<Order> findByIdAndShipperId(Long id, Long shipperId);
}
