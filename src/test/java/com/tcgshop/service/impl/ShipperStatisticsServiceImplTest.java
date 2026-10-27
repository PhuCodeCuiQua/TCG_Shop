package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.tcgshop.exception.BadRequestException;
import com.tcgshop.repository.ShipperStatisticsRepository;
import com.tcgshop.repository.ShipperStatisticsRepository.ShipperDeliveryCountRow;
import com.tcgshop.repository.ShipperStatisticsRepository.ShipperStatisticsTotals;
import com.tcgshop.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipperStatisticsServiceImplTest {
	private static final Long SHIPPER_ID = 31L;
	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-10-25T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

	@Mock
	private ShipperStatisticsRepository repository;
	@Mock
	private CurrentUser currentUser;
	private ShipperStatisticsServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ShipperStatisticsServiceImpl(repository, currentUser, CLOCK);
		lenient().when(currentUser.getId()).thenReturn(SHIPPER_ID);
	}

	@Test
	void summaryUsesCurrentShipperAndCalculatesSuccessRateAndCodTotal() {
		ShipperStatisticsTotals totals = totals(4L, 2L, 3L, new BigDecimal("540000"));
		when(repository.summarizeByShipper(SHIPPER_ID,
				LocalDate.of(2026, 9, 26).atStartOfDay(), LocalDate.of(2026, 10, 26).atStartOfDay()))
				.thenReturn(totals);

		var result = service.getStatistics(null, null);

		assertEquals(LocalDate.of(2026, 9, 26), result.fromDate());
		assertEquals(LocalDate.of(2026, 10, 25), result.toDate());
		assertEquals(4L, result.deliveredCount());
		assertEquals(2L, result.deliveryFailedCount());
		assertEquals(3L, result.inProgressCount());
		assertEquals(new BigDecimal("66.67"), result.successRate());
		assertEquals(new BigDecimal("540000"), result.codCollectedTotal());
		verify(repository).summarizeByShipper(SHIPPER_ID,
				LocalDate.of(2026, 9, 26).atStartOfDay(), LocalDate.of(2026, 10, 26).atStartOfDay());
	}

	@Test
	void successRateIsZeroWhenThereAreNoFinishedOrders() {
		when(repository.summarizeByShipper(SHIPPER_ID,
				LocalDate.of(2026, 9, 26).atStartOfDay(), LocalDate.of(2026, 10, 26).atStartOfDay()))
				.thenReturn(totals(0L, 0L, 0L, null));

		var result = service.getStatistics(null, null);

		assertEquals(new BigDecimal("0.00"), result.successRate());
		assertEquals(new BigDecimal("0.00"), result.codCollectedTotal());
	}

	@Test
	void dailyChartUsesCurrentShipperAndFillsMissingDaysWithZero() {
		ShipperDeliveryCountRow first = row("2026-10-23", 2L);
		ShipperDeliveryCountRow last = row("2026-10-25", 1L);
		when(repository.findDeliveredCountsByDay(SHIPPER_ID,
				LocalDate.of(2026, 10, 23).atStartOfDay(), LocalDate.of(2026, 10, 26).atStartOfDay()))
				.thenReturn(List.of(first, last));

		var result = service.getDailyDeliveries(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25));

		assertEquals(List.of("2026-10-23", "2026-10-24", "2026-10-25"), result.labels());
		assertEquals(List.of(2L, 0L, 1L), result.orderCounts());
		verify(repository).findDeliveredCountsByDay(SHIPPER_ID,
				LocalDate.of(2026, 10, 23).atStartOfDay(), LocalDate.of(2026, 10, 26).atStartOfDay());
	}

	@Test
	void chartReturnsSelectedDateRangeWithZeroCountsWhenThereAreNoDeliveredRows() {
		when(repository.findDeliveredCountsByDay(SHIPPER_ID,
				LocalDate.of(2026, 10, 1).atStartOfDay(), LocalDate.of(2026, 10, 4).atStartOfDay()))
				.thenReturn(List.of());

		var result = service.getDailyDeliveries(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

		assertEquals(List.of("2026-10-01", "2026-10-02", "2026-10-03"), result.labels());
		assertEquals(List.of(0L, 0L, 0L), result.orderCounts());
	}

	@Test
	void rejectsReversedDateRange() {
		assertThrows(BadRequestException.class,
				() -> service.getStatistics(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 25)));
	}

	private ShipperStatisticsTotals totals(Long delivered, Long failed, Long active, BigDecimal cod) {
		ShipperStatisticsTotals totals = org.mockito.Mockito.mock(ShipperStatisticsTotals.class);
		when(totals.getDeliveredCount()).thenReturn(delivered);
		when(totals.getDeliveryFailedCount()).thenReturn(failed);
		when(totals.getInProgressCount()).thenReturn(active);
		when(totals.getCodCollectedTotal()).thenReturn(cod);
		return totals;
	}

	private ShipperDeliveryCountRow row(String date, Long count) {
		ShipperDeliveryCountRow row = org.mockito.Mockito.mock(ShipperDeliveryCountRow.class);
		when(row.getDeliveryDate()).thenReturn(date);
		when(row.getOrderCount()).thenReturn(count);
		return row;
	}
}
