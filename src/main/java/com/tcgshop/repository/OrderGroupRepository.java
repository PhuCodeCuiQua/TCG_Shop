package com.tcgshop.repository;

import com.tcgshop.entity.OrderGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

	Optional<OrderGroup> findByGroupCode(String groupCode);

	Optional<OrderGroup> findByIdempotencyToken(String idempotencyToken);
}
