package com.tcgshop.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.event.OrderAssignedEvent;
import com.tcgshop.event.OrderStatusChangedEvent;
import com.tcgshop.service.NotificationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private OrderNotificationListener listener;

	@Test
	void deliveredStatusNotifiesBuyerAndShopWithOrderLinks() {
		listener.onOrderStatusChanged(statusEvent(OrderStatus.DELIVERED, 101L, null));

		verify(notificationService).send(10L, NotificationType.ORDER_STATUS, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã chuyển sang trạng thái: Đã giao.",
				"/orders/101");
		verify(notificationService).send(20L, NotificationType.ORDER_STATUS, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã chuyển sang trạng thái: Đã giao.",
				"/vendor/orders/101");
		verifyNoMoreInteractions(notificationService);
	}

	@Test
	void cancelledStatusIncludesReasonAndDoesNotNotifyActor() {
		listener.onOrderStatusChanged(statusEvent(OrderStatus.CANCELLED, 20L, "Khách yêu cầu hủy"));

		verify(notificationService).send(10L, NotificationType.ORDER_STATUS, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã chuyển sang trạng thái: Đã hủy. Lý do: Khách yêu cầu hủy",
				"/orders/101");
		verify(notificationService).send(30L, NotificationType.ORDER_STATUS, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã chuyển sang trạng thái: Đã hủy. Lý do: Khách yêu cầu hủy",
				"/shipper/dashboard");
		verify(notificationService, never()).send(20L, NotificationType.ORDER_STATUS, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã chuyển sang trạng thái: Đã hủy. Lý do: Khách yêu cầu hủy",
				"/vendor/orders/101");
		verifyNoMoreInteractions(notificationService);
	}

	@Test
	void assignmentNotifiesShipperAndBuyer() {
		listener.onOrderAssigned(new OrderAssignedEvent(101L, "TCG-101", 30L, 10L, 40L));

		verify(notificationService).send(30L, NotificationType.ORDER_ASSIGNED, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã được phân công cho bạn.", "/shipper/dashboard");
		verify(notificationService).send(10L, NotificationType.ORDER_ASSIGNED, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã được phân công cho đơn vị giao hàng.", "/orders/101");
		verifyNoMoreInteractions(notificationService);
	}

	@Test
	void assignmentDoesNotNotifyActorWhenActorIsOneOfRecipients() {
		listener.onOrderAssigned(new OrderAssignedEvent(101L, "TCG-101", 30L, 10L, 30L));

		verify(notificationService, never()).send(30L, NotificationType.ORDER_ASSIGNED, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã được phân công cho bạn.", "/shipper/dashboard");
		verify(notificationService).send(10L, NotificationType.ORDER_ASSIGNED, "Đơn TCG-101",
				"Đơn hàng #TCG-101 đã được phân công cho đơn vị giao hàng.", "/orders/101");
		verifyNoMoreInteractions(notificationService);
	}

	private OrderStatusChangedEvent statusEvent(OrderStatus newStatus, Long actorId, String note) {
		return new OrderStatusChangedEvent(101L, "TCG-101", 10L, 5L, 20L, 30L,
				OrderStatus.SHIPPING, newStatus, actorId, note, LocalDateTime.now());
	}
}
