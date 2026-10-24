package com.tcgshop.listener;

import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.event.OrderGroupPlacedEvent;
import com.tcgshop.service.EmailService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPlacedEmailListener {
	private static final Logger log = LoggerFactory.getLogger(OrderPlacedEmailListener.class);
	private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
	private final EmailService emailService;

	public OrderPlacedEmailListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderGroupPlaced(OrderGroupPlacedEvent event) {
		List<String> paragraphs = new ArrayList<>();
		for (String orderCode : event.orderCodes()) {
			paragraphs.add("Mã đơn hàng của cửa hàng: " + orderCode);
		}
		paragraphs.add("Tổng thanh toán: " + formatVnd(event.grandTotal()));
		paragraphs.add("Phương thức thanh toán: " + paymentLabel(event.paymentMethod()));
		paragraphs.add("Bạn có thể theo dõi đơn hàng tại mục Đơn hàng.");
		try {
			emailService.sendNotificationEmail(event.buyerEmail(), "Xác nhận đơn hàng " + event.groupCode(),
					"Cảm ơn bạn đã đặt hàng tại TCG Shop", paragraphs);
		} catch (Exception exception) {
			// Email là tác vụ phụ sau commit; lỗi gửi chỉ được ghi log và không làm ảnh hưởng đơn đã đặt.
			log.warn("Could not send order confirmation email for groupCode={} ({})",
					event.groupCode(), exception.getClass().getSimpleName());
		}
	}

	private String paymentLabel(PaymentMethod method) {
		return method == PaymentMethod.COD ? "Thanh toán khi nhận hàng" : method.getDisplayName();
	}

	private String formatVnd(BigDecimal amount) {
		return NumberFormat.getCurrencyInstance(VIETNAMESE).format(amount == null ? BigDecimal.ZERO : amount);
	}
}
