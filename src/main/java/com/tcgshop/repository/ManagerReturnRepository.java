package com.tcgshop.repository;

import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy vấn khiếu nại trả hàng cho Manager/Admin (PA-21). */
public interface ManagerReturnRepository extends JpaRepository<ReturnRequest, Long> {

	@Query(value = "select r from ReturnRequest r join fetch r.order o join fetch r.user u join fetch o.shop s "
			+ "where o.status = :orderStatus and r.status = :returnStatus order by r.createdAt desc",
			countQuery = "select count(r) from ReturnRequest r join r.order o "
					+ "where o.status = :orderStatus and r.status = :returnStatus")
	Page<ReturnRequest> findByStates(@Param("orderStatus") OrderStatus orderStatus,
			@Param("returnStatus") ReturnStatus returnStatus, Pageable pageable);

	@Query(value = "select r from ReturnRequest r join fetch r.order o join fetch r.user u join fetch o.shop s "
			+ "where o.status = :orderStatus order by r.createdAt desc",
			countQuery = "select count(r) from ReturnRequest r join r.order o where o.status = :orderStatus")
	Page<ReturnRequest> findByOrderStatus(@Param("orderStatus") OrderStatus orderStatus, Pageable pageable);

	@Query("select r from ReturnRequest r join fetch r.order o join fetch r.user u join fetch o.shop s "
			+ "where o.id = :orderId")
	Optional<ReturnRequest> findDetailByOrderId(@Param("orderId") Long orderId);
}