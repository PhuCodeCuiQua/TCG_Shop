package com.tcgshop.listener;

import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.event.OrderCreatedEvent;
import com.tcgshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Gửi thông báo cho chủ shop sau khi đơn con đã được lưu thành công. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VendorOrderNotificationListener {

	private final NotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderCreated(OrderCreatedEvent event) {
		if (event.shopOwnerId() == null) {
			log.warn("Bỏ qua thông báo đơn {} vì sự kiện không có shopOwnerId", event.orderId());
			return;
		}

		String content = "Bạn có đơn hàng mới #" + event.orderCode();
		try {
			notificationService.send(
					event.shopOwnerId(),
					NotificationType.ORDER_NEW,
					"Đơn hàng mới",
					content,
					"/vendor/orders/" + event.orderId());

		} catch (Exception exception) {
			// Listener chạy sau commit; lỗi thông báo chỉ được ghi log, không ảnh hưởng đơn hàng.
			log.error("Không thể gửi thông báo đơn mới {} tới chủ shop {}",
					event.orderId(), event.shopOwnerId(), exception);
		}
	}
}
