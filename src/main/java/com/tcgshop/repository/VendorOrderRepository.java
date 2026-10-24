package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Vendor-only order queries scoped by the current shop id supplied by the service. */
public interface VendorOrderRepository extends JpaRepository<Order, Long> {
	@Query(value = """
			select o from Order o
			left join fetch o.user buyer
			left join fetch o.orderGroup payment
			where o.shop.id = :shopId
				and o.status in :statuses
				and (:keyword is null or lower(o.orderCode) like lower(concat('%', :keyword, '%')))
			""",
			countQuery = """
			select count(o) from Order o
			where o.shop.id = :shopId
				and o.status in :statuses
				and (:keyword is null or lower(o.orderCode) like lower(concat('%', :keyword, '%')))
			""")
	Page<Order> searchShopOrders(@Param("shopId") Long shopId,
			@Param("statuses") Collection<OrderStatus> statuses,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("select o.status as status, count(o.id) as total from Order o where o.shop.id = :shopId group by o.status")
	List<VendorOrderStatusCount> countStatusesByShopId(@Param("shopId") Long shopId);

	@Query("""
			select distinct o from Order o
			join fetch o.shop shop
			left join fetch shop.owner
			left join fetch o.user buyer
			left join fetch o.orderGroup payment
			left join fetch o.items item
			where o.id = :orderId
			""")
	Optional<Order> findDetailById(@Param("orderId") Long orderId);
}
