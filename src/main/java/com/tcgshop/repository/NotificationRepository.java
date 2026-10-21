package com.tcgshop.repository;

import com.tcgshop.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserIdAndReadFalse(Long userId);
}
