package com.tcgshop.service;

import com.tcgshop.dto.response.NotificationResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.UnreadCountResponse;
import com.tcgshop.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

	NotificationResponse send(Long userId, NotificationType type, String title, String content, String link);

	void broadcast(String destination, Object payload);

	PageResponse<NotificationResponse> getMyNotifications(Long currentUserId, Pageable pageable);

	UnreadCountResponse countUnread(Long currentUserId);

	NotificationResponse markAsRead(Long currentUserId, Long notificationId);

	int markAllAsRead(Long currentUserId);
}