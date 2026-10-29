package com.tcgshop.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.event.OrderGroupPlacedEvent;
import com.tcgshop.service.EmailService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPlacedEmailListenerTest {
	@Mock private EmailService emailService;
	@InjectMocks private OrderPlacedEmailListener listener;

	@Test
	void sendsConfirmationToBuyerWithExpectedSubjectAndCodLabel() {
		OrderGroupPlacedEvent event = event();

		listener.onOrderGroupPlaced(event);

		ArgumentCaptor<List<String>> paragraphs = ArgumentCaptor.forClass(List.class);
		verify(emailService).sendNotificationEmail(eq("buyer@example.test"), eq("Xác nhận đơn hàng GRP123"),
				eq("Cảm ơn bạn đã đặt hàng tại TCG Shop"), paragraphs.capture());
		assertTrue(paragraphs.getValue().contains("Mã đơn hàng của cửa hàng: TCG123"));
		assertTrue(paragraphs.getValue().contains("Phương thức thanh toán: Thanh toán khi nhận hàng"));
		assertTrue(paragraphs.getValue().contains("Bạn có thể theo dõi đơn hàng tại mục Đơn hàng."));
	}

	@Test
	void emailFailureIsLoggedAndDoesNotEscapeListener() {
		doThrow(new IllegalStateException("mail unavailable")).when(emailService)
				.sendNotificationEmail(eq("buyer@example.test"), eq("Xác nhận đơn hàng GRP123"),
					eq("Cảm ơn bạn đã đặt hàng tại TCG Shop"), anyList());

		assertDoesNotThrow(() -> listener.onOrderGroupPlaced(event()));
	}

	private static OrderGroupPlacedEvent event() {
		return new OrderGroupPlacedEvent(1L, "GRP123", 2L, "buyer@example.test", "Nguyen Van A",
				PaymentMethod.COD, new BigDecimal("123000"), List.of("TCG123"));
	}
}
