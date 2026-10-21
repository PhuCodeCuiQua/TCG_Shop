package com.tcgshop.repository;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

	Page<Order> findByShopIdAndStatus(Long shopId, OrderStatus status, Pageable pageable);

	Page<Order> findByShipperIdAndStatus(Long shipperId, OrderStatus status, Pageable pageable);

	Optional<Order> findByOrderCode(String orderCode);

	Page<Order> findByOrderGroupId(Long orderGroupId, Pageable pageable);
}
