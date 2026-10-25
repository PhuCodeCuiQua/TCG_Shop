package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

	@Override
	@EntityGraph(attributePaths = {"shop", "orderGroup"})
	Page<Order> findAll(Specification<Order> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"shop", "shippingCarrier", "shipper", "orderGroup"})
	Optional<Order> findDetailByIdAndUserId(Long id, Long userId);

	@Query("select o.status as status, count(o.id) as total from Order o "
			+ "where o.user.id = :userId group by o.status")
	List<StatusCount> countGroupedByStatus(@Param("userId") Long userId);

	Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

	Page<Order> findByShopIdAndStatus(Long shopId, OrderStatus status, Pageable pageable);

	Page<Order> findByShipperIdAndStatus(Long shipperId, OrderStatus status, Pageable pageable);

	long countByShipper_IdAndStatus(Long shipperId, OrderStatus status);

	Optional<Order> findByOrderCode(String orderCode);

	Optional<Order> findByIdAndUserId(Long id, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :id and o.user.id = :userId")
	Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

	Page<Order> findByOrderGroupId(Long orderGroupId, Pageable pageable);

	boolean existsByOrderCode(String orderCode);

	@EntityGraph(attributePaths = "shop")
	List<Order> findByOrderGroupIdOrderByIdAsc(Long orderGroupId);

	@EntityGraph(attributePaths = "shop")
	List<Order> findByOrderGroupIdAndIdNotAndUserIdOrderByIdAsc(Long groupId, Long excludeId, Long userId);
}
