package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.NotificationResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.UnreadCountResponse;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST thông báo của chính người đang đăng nhập.
 * userId luôn lấy từ CurrentUser (SecurityContext), không nhận từ client.
 */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationRestController {

	private static final int MAX_PAGE_SIZE = 50;

	private final NotificationService notificationService;
	private final CurrentUser currentUser;

	@GetMapping
	public ResponseEntity<PageResponse<NotificationResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		// Giới hạn size để client không xin quá nhiều bản ghi một lần
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
		return ResponseEntity.ok(notificationService.getMyNotifications(currentUser.getId(), pageable));
	}

	@GetMapping("/unread-count")
	public ResponseEntity<UnreadCountResponse> unreadCount() {
		return ResponseEntity.ok(notificationService.countUnread(currentUser.getId()));
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
		return ResponseEntity.ok(notificationService.markAsRead(currentUser.getId(), id));
	}

	@PatchMapping("/read-all")
	public ResponseEntity<Map<String, Integer>> markAllAsRead() {
		int updated = notificationService.markAllAsRead(currentUser.getId());
		return ResponseEntity.ok(Map.of("updated", updated));
	}
}