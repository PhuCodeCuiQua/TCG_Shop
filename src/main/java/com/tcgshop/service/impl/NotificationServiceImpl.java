package com.tcgshop.service.impl;

import com.tcgshop.dto.response.NotificationResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.UnreadCountResponse;
import com.tcgshop.entity.Notification;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.NotificationMapper;
import com.tcgshop.repository.NotificationRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
	private static final String PUBLIC_TOPIC_PREFIX = "/topic/";

	private final NotificationRepository notificationRepository;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationMapper notificationMapper;
	private final SimpMessagingTemplate messagingTemplate;

	@Override
	@Transactional
	public NotificationResponse send(Long userId, NotificationType type, String title, String content,
			String link) {
		User recipient = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người nhận thông báo"));

		// Bước 1: ghi DB trước (AGENTS mục 12: ghi bảng notifications rồi mới đẩy real-time)
		Notification notification = new Notification();
		notification.setUser(recipient);
		notification.setType(type);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setLink(link);
		Notification saved = notificationRepository.save(notification);
		NotificationResponse response = notificationMapper.toResponse(saved);

		// Bước 2: đẩy real-time SAU KHI transaction commit, tránh báo cho dữ liệu bị rollback.
		// Principal của WebSocket là email (CustomUserDetails.getUsername()) nên truyền email.
		String principalName = recipient.getEmail();
		runAfterCommit(() -> pushToUser(principalName, response));
		return response;
	}

	@Override
	public void broadcast(String destination, Object payload) {
		// Chỉ cho phát lên /topic/** để không vô tình bắn vào kênh khác
		if (destination == null || !destination.startsWith(PUBLIC_TOPIC_PREFIX)) {
			throw new IllegalArgumentException("Broadcast chỉ được dùng destination bắt đầu bằng /topic/");
		}
		try {
			messagingTemplate.convertAndSend(destination, payload);
		} catch (Exception ex) {
			// Lỗi đẩy real-time chỉ log, không làm hỏng nghiệp vụ chính
			log.warn("Không thể broadcast tới {}: {}", destination, ex.getMessage());
		}
	}

	@Override
	@Transactional
	public PageResponse<NotificationResponse> getMyNotifications(Long currentUserId, Pageable pageable) {
		return PageResponse.from(notificationRepository
				.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable)
				.map(notification -> {
					repairLegacyLink(notification);
					return notificationMapper.toResponse(notification);
				}));
	}

	@Override
	@Transactional(readOnly = true)
	public UnreadCountResponse countUnread(Long currentUserId) {
		return new UnreadCountResponse(notificationRepository.countByUserIdAndIsReadFalse(currentUserId));
	}

	@Override
	@Transactional
	public NotificationResponse markAsRead(Long currentUserId, Long notificationId) {
		// Ownership: chỉ tìm trong thông báo của chính người đang đăng nhập
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUserId)
				.orElseThrow(() -> {
					// Tồn tại nhưng của người khác -> 403; không tồn tại -> 404
					if (notificationRepository.existsById(notificationId)) {
						return new ForbiddenException("Bạn không có quyền với thông báo này");
					}
					return new ResourceNotFoundException("Không tìm thấy thông báo");
				});
		repairLegacyLink(notification);
		notification.setRead(true); // dirty checking tự UPDATE khi commit
		return notificationMapper.toResponse(notification);
	}

	/** Tự sửa link legacy đã lưu sai trước đây, ngay khi thông báo được người nhận tải/xem. */
	private void repairLegacyLink(Notification notification) {
		String currentLink = notification.getLink();
		if (currentLink != null && !currentLink.isBlank() && !"/profile".equals(currentLink)) {
			return;
		}

		String correctedLink = null;
		if (notification.getType() == NotificationType.SHOP_APPROVAL) {
			correctedLink = "/vendor/shop/manage";
		} else if (notification.getType() == NotificationType.SYSTEM
				&& ("Sản phẩm đã bị gỡ khỏi hệ thống".equals(notification.getTitle())
						|| "Sản phẩm đã được khôi phục".equals(notification.getTitle()))) {
			correctedLink = "/vendor/products";
		} else if (notification.getType() == NotificationType.ORDER_NEW
				&& "Đơn hàng mới".equals(notification.getTitle())) {
			correctedLink = resolveVendorOrderLink(notification.getContent());
		}

		if (correctedLink != null) {
			notification.setLink(correctedLink);
		}
	}

	private String resolveVendorOrderLink(String content) {
		if (content == null || !content.startsWith("Bạn có đơn hàng mới #")) {
			return null;
		}
		int codeStart = content.lastIndexOf('#') + 1;
		String orderCode = content.substring(codeStart).trim();
		if (orderCode.isEmpty()) {
			return null;
		}
		return orderRepository.findByOrderCode(orderCode)
				.map(Order::getId)
				.map(id -> "/vendor/orders/" + id)
				.orElse(null);
	}

	@Override
	@Transactional
	public int markAllAsRead(Long currentUserId) {
		return notificationRepository.markAllAsReadByUserId(currentUserId, LocalDateTime.now());
	}

	private void runAfterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}

	private void pushToUser(String principalName, NotificationResponse response) {
		try {
			// Spring tự thêm prefix /user => client nghe /user/queue/notifications
			messagingTemplate.convertAndSendToUser(principalName, USER_NOTIFICATION_DESTINATION, response);
		} catch (Exception ex) {
			log.warn("Không thể đẩy thông báo real-time cho {}: {}", principalName, ex.getMessage());
		}
	}
}
