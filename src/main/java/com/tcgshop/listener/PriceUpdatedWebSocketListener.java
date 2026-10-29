package com.tcgshop.listener;

import com.tcgshop.dto.response.PriceUpdateResponse;
import com.tcgshop.event.PriceUpdatedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdatedWebSocketListener {

	private static final String PRICE_UPDATE_TOPIC = "/topic/price-update/";
	private static final int PERCENT_SCALE = 2;
	private static final BigDecimal PERCENT_MULTIPLIER = BigDecimal.valueOf(100);

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPriceUpdated(PriceUpdatedEvent event) {
		PriceUpdateResponse response = new PriceUpdateResponse(
				event.cardId(),
				event.cardName(),
				event.oldPrice(),
				event.newPrice(),
				calculateChangePercent(event.oldPrice(), event.newPrice()),
				event.recordDate(),
				event.source(),
				LocalDateTime.now());

		try {
			messagingTemplate.convertAndSend(PRICE_UPDATE_TOPIC + event.cardId(), response);
		} catch (RuntimeException exception) {
			log.warn("Không thể phát biến động giá WebSocket cho cardId={}", event.cardId(), exception);
		}
	}

	private BigDecimal calculateChangePercent(BigDecimal oldPrice, BigDecimal newPrice) {
		if (oldPrice == null || newPrice == null || oldPrice.signum() == 0) {
			return null;
		}
		return newPrice.subtract(oldPrice)
				.multiply(PERCENT_MULTIPLIER)
				.divide(oldPrice, PERCENT_SCALE, RoundingMode.HALF_UP);
	}
}
