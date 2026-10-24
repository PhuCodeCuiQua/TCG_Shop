package com.tcgshop.repository;

import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
	@Query("select h.previousStatus as previousStatus, h.newStatus as newStatus, h.note as note, "
			+ "h.createdAt as createdAt, actor.id as actorId from OrderStatusHistory h "
			+ "left join h.changedBy actor where h.order.id = :orderId "
			+ "order by h.createdAt asc, h.id asc")
	List<TimelineRow> findTimelineByOrderId(@Param("orderId") Long orderId);
}
