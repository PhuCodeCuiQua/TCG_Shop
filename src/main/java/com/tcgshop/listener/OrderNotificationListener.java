package com.tcgshop.listener;

import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.event.OrderAssignedEvent;
import com.tcgshop.event.OrderConfirmedEvent;
import com.tcgshop.event.OrderStatusChangedEvent;
import com.tcgshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Gửi thông báo đơn hàng sau commit để lưu DB và đẩy tới WebSocket an toàn. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

	private static final String ORDER_TITLE_PREFIX = "Đơn ";
	private static final String BUYER_ORDER_LINK = "/orders/";
	private static final String VENDOR_ORDER_LINK = "/vendor/orders/";
	private static final String SHIPPER_ORDER_LINK = "/shipper/orders/";

	private final NotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderStatusChanged(OrderStatusChangedEvent event) {
		String title = ORDER_TITLE_PREFIX + event.orderCode();
		String content = buildStatusContent(event);
		String orderId = String.valueOf(event.orderId());

		switch (event.newStatus()) {
			case CONFIRMED, PICKED_UP, SHIPPING ->
					send(event.buyerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
							content, BUYER_ORDER_LINK + orderId, event.orderId());
			case DELIVERED, DELIVERY_FAILED, RETURNED ->
					sendStatusToBuyerAndShop(event, title, content, orderId);
			case CANCELLED -> {
				sendStatusToBuyerAndShop(event, title, content, orderId);
				send(event.shipperId(), event.actorId(), NotificationType.ORDER_STATUS, title,
						content, SHIPPER_ORDER_LINK + orderId, event.orderId());
			}
			case RETURN_REQUESTED ->
					send(event.shopOwnerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
							content, VENDOR_ORDER_LINK + orderId, event.orderId());
			case RETURN_APPROVED -> {
				send(event.buyerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
						content, BUYER_ORDER_LINK + orderId, event.orderId());
				send(event.shipperId(), event.actorId(), NotificationType.ORDER_STATUS, title,
						content, SHIPPER_ORDER_LINK + orderId, event.orderId());
			}
			case RETURN_REJECTED, REFUNDED ->
					send(event.buyerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
							content, BUYER_ORDER_LINK + orderId, event.orderId());
			case NEW -> {
				// NEW không có thông báo chuyển trạng thái theo chính sách hiện tại.
			}
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderAssigned(OrderAssignedEvent event) {
		String title = ORDER_TITLE_PREFIX + event.orderCode();
		String shipperContent = "Đơn hàng #" + event.orderCode() + " đã được phân công cho bạn.";
		send(event.shipperId(), event.assignedById(), NotificationType.ORDER_ASSIGNED, title,
				shipperContent, SHIPPER_ORDER_LINK + event.orderId(), event.orderId());

		String buyerContent = "Đơn hàng #" + event.orderCode() + " đã được phân công cho đơn vị giao hàng.";
		send(event.buyerId(), event.assignedById(), NotificationType.ORDER_ASSIGNED, title,
				buyerContent, BUYER_ORDER_LINK + event.orderId(), event.orderId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderConfirmed(OrderConfirmedEvent event) {
		String content = "Đơn hàng #" + event.orderCode()
				+ " đã được xác nhận, chờ phân công shipper.";
		for (Long recipientId : event.recipientIds()) {
			send(recipientId, null, NotificationType.ORDER_STATUS, "Đơn hàng chờ phân công",
					content, "/manager/shipper-assignment", event.orderId());
		}
	}

	private void sendStatusToBuyerAndShop(OrderStatusChangedEvent event, String title,
			String content, String orderId) {
		send(event.buyerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
				content, BUYER_ORDER_LINK + orderId, event.orderId());
		send(event.shopOwnerId(), event.actorId(), NotificationType.ORDER_STATUS, title,
				content, VENDOR_ORDER_LINK + orderId, event.orderId());
	}

	private String buildStatusContent(OrderStatusChangedEvent event) {
		String content = "Đơn hàng #" + event.orderCode() + " đã chuyển sang trạng thái: "
				+ event.newStatus().getDisplayName() + ".";
		if (event.note() != null && !event.note().isBlank()
				&& (event.newStatus() == OrderStatus.DELIVERY_FAILED
						|| event.newStatus() == OrderStatus.CANCELLED
						|| event.newStatus() == OrderStatus.RETURN_REJECTED)) {
			content += " Lý do: " + event.note().trim();
		}
		return content;
	}

	private void send(Long recipientId, Long actorId, NotificationType type, String title,
			String content, String link, Long orderId) {
		if (recipientId == null || recipientId.equals(actorId)) {
			return;
		}
		try {
			notificationService.send(recipientId, type, title, content, link);
		} catch (Exception exception) {
			log.error("Không thể gửi thông báo đơn {} tới người dùng {}",
					orderId, recipientId, exception);
		}
	}
}
