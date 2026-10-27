package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ShipperDeliveryChartResponse;
import com.tcgshop.dto.response.ShipperStatisticsResponse;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.repository.ShipperStatisticsRepository;
import com.tcgshop.repository.ShipperStatisticsRepository.ShipperDeliveryCountRow;
import com.tcgshop.repository.ShipperStatisticsRepository.ShipperStatisticsTotals;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShipperStatisticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperStatisticsServiceImpl implements ShipperStatisticsService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final long DEFAULT_RANGE_DAYS = 30;
	private static final long MAX_RANGE_DAYS = 366;
	private final ShipperStatisticsRepository shipperStatisticsRepository;
	private final CurrentUser currentUser;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public ShipperStatisticsResponse getStatistics(LocalDate fromDate, LocalDate toDate) {
		DateRange range = resolveRange(fromDate, toDate);
		Long shipperId = currentUser.getId();
		ShipperStatisticsTotals totals = shipperStatisticsRepository.summarizeByShipper(
				shipperId, range.startInclusive(), range.endExclusive());

		long delivered = count(totals == null ? null : totals.getDeliveredCount());
		long failed = count(totals == null ? null : totals.getDeliveryFailedCount());
		long inProgress = count(totals == null ? null : totals.getInProgressCount());
		long finished = delivered + failed;
		BigDecimal successRate = finished == 0
				? ZERO.setScale(2)
				: BigDecimal.valueOf(delivered).multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(finished), 2, RoundingMode.HALF_UP);
		BigDecimal codCollected = totals == null || totals.getCodCollectedTotal() == null
				? ZERO.setScale(2) : totals.getCodCollectedTotal();
		return new ShipperStatisticsResponse(range.fromDate(), range.toDate(), delivered,
				failed, inProgress, successRate, codCollected);
	}

	@Override
	@Transactional(readOnly = true)
	public ShipperDeliveryChartResponse getDailyDeliveries(LocalDate fromDate, LocalDate toDate) {
		DateRange range = resolveRange(fromDate, toDate);
		List<ShipperDeliveryCountRow> rows = shipperStatisticsRepository.findDeliveredCountsByDay(
				currentUser.getId(), range.startInclusive(), range.endExclusive());

		Map<LocalDate, Long> countsByDate = new HashMap<>();
		if (rows != null) {
			for (ShipperDeliveryCountRow row : rows) {
				if (row == null || row.getDeliveryDate() == null) continue;
				try {
					countsByDate.put(LocalDate.parse(row.getDeliveryDate()), count(row.getOrderCount()));
				} catch (RuntimeException exception) {
					throw new IllegalStateException("Không thể đọc ngày giao hàng từ dữ liệu thống kê", exception);
				}
			}
		}

		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();
		for (LocalDate day = range.fromDate(); !day.isAfter(range.toDate()); day = day.plusDays(1)) {
			labels.add(day.toString());
			values.add(countsByDate.getOrDefault(day, 0L));
		}
		return new ShipperDeliveryChartResponse(labels, values);
	}

	private DateRange resolveRange(LocalDate fromDate, LocalDate toDate) {
		LocalDate today = LocalDate.now(clock);
		LocalDate resolvedTo = toDate == null ? today : toDate;
		if (fromDate == null && toDate != null
				&& toDate.isBefore(LocalDate.MIN.plusDays(DEFAULT_RANGE_DAYS - 1))) {
			throw new BadRequestException("Ngày kết thúc không hợp lệ");
		}
		LocalDate resolvedFrom = fromDate == null ? resolvedTo.minusDays(DEFAULT_RANGE_DAYS - 1) : fromDate;
		if (resolvedFrom.isAfter(resolvedTo)) {
			throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
		}
		if (resolvedTo.equals(LocalDate.MAX)) {
			throw new BadRequestException("Ngày kết thúc không hợp lệ");
		}
		if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) > MAX_RANGE_DAYS) {
			throw new BadRequestException("Khoảng thời gian thống kê không được vượt quá 366 ngày");
		}
		return new DateRange(resolvedFrom, resolvedTo,
				resolvedFrom.atStartOfDay(), resolvedTo.plusDays(1).atStartOfDay());
	}

	private long count(Long value) {
		return value == null ? 0L : Math.max(value, 0L);
	}

	private record DateRange(LocalDate fromDate, LocalDate toDate,
			LocalDateTime startInclusive, LocalDateTime endExclusive) { }
}
