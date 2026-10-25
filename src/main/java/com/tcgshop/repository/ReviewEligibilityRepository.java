package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only order queries used to decide which delivered purchases can receive a review. */
public interface ReviewEligibilityRepository extends Repository<Order, Long> {
	Optional<Order> findById(Long id);

	@Query("select count(distinct o.id) from Order o join o.items item "
			+ "where o.user.id = :userId and item.product.id = :productId "
			+ "and o.status = com.tcgshop.entity.enums.OrderStatus.DELIVERED")
	long countDeliveredPurchases(@Param("userId") Long userId, @Param("productId") Long productId);

	@Query("select o.id from Order o join o.items item "
			+ "where o.user.id = :userId and item.product.id = :productId "
			+ "and o.status = com.tcgshop.entity.enums.OrderStatus.DELIVERED "
			+ "and not exists (select review.id from Review review "
			+ "where review.order.id = o.id and review.product.id = :productId) "
			+ "order by o.deliveredAt desc, o.id desc")
	List<Long> findEligibleOrderIds(@Param("userId") Long userId,
			@Param("productId") Long productId, Pageable pageable);
}
