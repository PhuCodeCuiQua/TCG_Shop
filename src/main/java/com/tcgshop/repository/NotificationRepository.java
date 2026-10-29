package com.tcgshop.repository;

import com.tcgshop.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserIdAndIsReadFalse(Long userId);

	// Danh sách phân trang của một người dùng, mới nhất trước (Pageable không cần kèm Sort)
	Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	// Tìm thông báo theo id VÀ chủ sở hữu: kiểm tra ownership ngay trong câu truy vấn
	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	// Đánh dấu tất cả đã đọc bằng một câu UPDATE; chỉ tác động thông báo của userId
	@Modifying(clearAutomatically = true)
	@Query("""
			update Notification n
			set n.isRead = true, n.updatedAt = :now
			where n.user.id = :userId and n.isRead = false
			""")
	int markAllAsReadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}