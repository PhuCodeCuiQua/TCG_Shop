package com.tcgshop.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcgshop.dto.response.PriceUpdateResponse;
import com.tcgshop.event.PriceUpdatedEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class PriceUpdatedWebSocketListenerTest {

	@Mock private SimpMessagingTemplate messagingTemplate;
	@InjectMocks private PriceUpdatedWebSocketListener listener;

	@Test
	void publishesPriceUpdateWithPercentageAndTimestamp() {
		PriceUpdatedEvent event = new PriceUpdatedEvent(
				12L, "Pikachu", new BigDecimal("100.00"), new BigDecimal("125.00"),
				LocalDate.of(2025, 3, 10), "ADMIN");
		ArgumentCaptor<PriceUpdateResponse> responseCaptor = ArgumentCaptor.forClass(PriceUpdateResponse.class);

		listener.onPriceUpdated(event);

		verify(messagingTemplate).convertAndSend(eq("/topic/price-update/12"), responseCaptor.capture());
		PriceUpdateResponse response = responseCaptor.getValue();
		assertThat(response.cardId()).isEqualTo(12L);
		assertThat(response.cardName()).isEqualTo("Pikachu");
		assertThat(response.oldPrice()).isEqualByComparingTo("100.00");
		assertThat(response.newPrice()).isEqualByComparingTo("125.00");
		assertThat(response.changePercent()).isEqualByComparingTo("25.00");
		assertThat(response.recordDate()).isEqualTo(event.recordDate());
		assertThat(response.source()).isEqualTo("ADMIN");
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	void publishesNullPercentageWhenOldPriceIsZeroOrNewPriceIsMissing() {
		listener.onPriceUpdated(new PriceUpdatedEvent(
				12L, "Pikachu", BigDecimal.ZERO, new BigDecimal("125.00"),
				LocalDate.of(2025, 3, 10), "ADMIN"));
		listener.onPriceUpdated(new PriceUpdatedEvent(
				12L, "Pikachu", new BigDecimal("125.00"), null,
				LocalDate.of(2025, 3, 10), "ADMIN"));

		ArgumentCaptor<PriceUpdateResponse> responseCaptor = ArgumentCaptor.forClass(PriceUpdateResponse.class);
		verify(messagingTemplate, times(2))
				.convertAndSend(eq("/topic/price-update/12"), responseCaptor.capture());
		assertThat(responseCaptor.getAllValues())
				.extracting(PriceUpdateResponse::changePercent)
				.containsExactly(null, null);
	}

	@Test
	void calculatesNegativePercentageForPriceDecrease() {
		listener.onPriceUpdated(new PriceUpdatedEvent(
				12L, "Pikachu", new BigDecimal("200.00"), new BigDecimal("150.00"),
				LocalDate.of(2025, 3, 10), "JOB"));

		ArgumentCaptor<PriceUpdateResponse> responseCaptor = ArgumentCaptor.forClass(PriceUpdateResponse.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/price-update/12"), responseCaptor.capture());
		assertThat(responseCaptor.getValue().changePercent()).isEqualByComparingTo("-25.00");
	}
}
