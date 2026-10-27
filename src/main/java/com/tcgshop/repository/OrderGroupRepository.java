package com.tcgshop.repository;

import com.tcgshop.entity.OrderGroup;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

	Optional<OrderGroup> findByGroupCode(String groupCode);

	Optional<OrderGroup> findByIdempotencyToken(String idempotencyToken);

	boolean existsByGroupCode(String groupCode);

	Optional<OrderGroup> findByGroupCodeAndUserId(String groupCode, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from OrderGroup g where g.groupCode = :groupCode and g.user.id = :userId")
	Optional<OrderGroup> findByGroupCodeAndUserIdForUpdate(
			@Param("groupCode") String groupCode, @Param("userId") Long userId);
}
