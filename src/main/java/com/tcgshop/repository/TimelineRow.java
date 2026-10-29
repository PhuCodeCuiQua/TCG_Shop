package com.tcgshop.repository;

import com.tcgshop.entity.enums.OrderStatus;
import java.time.LocalDateTime;

public interface TimelineRow {
	OrderStatus getPreviousStatus();
	OrderStatus getNewStatus();
	String getNote();
	LocalDateTime getCreatedAt();
	Long getActorId();
}
