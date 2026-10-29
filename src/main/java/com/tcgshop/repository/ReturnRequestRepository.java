package com.tcgshop.repository;

import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.enums.ReturnStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
	Optional<ReturnRequest> findByOrderId(Long orderId);

	boolean existsByOrderId(Long orderId);

	@EntityGraph(attributePaths = {"order", "order.shop", "order.user"})
	Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);
}
