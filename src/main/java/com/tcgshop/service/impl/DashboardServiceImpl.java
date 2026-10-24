package com.tcgshop.service.impl;

import com.tcgshop.dto.response.DashboardSummaryResponse;
import com.tcgshop.dto.response.OrderStatusCountResponse;
import com.tcgshop.dto.response.RevenuePointResponse;
import com.tcgshop.dto.response.TopProductResponse;
import com.tcgshop.dto.response.TopShopResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.repository.DashboardRepository;
import com.tcgshop.repository.DashboardRepository.DeliveredRow;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.DashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
	private static final int MAX_DAYS = 366;
	private static final int DEFAULT_DAYS = 30;
	private static final int MAX_LIMIT = 20;

	private final DashboardRepository dashboardRepository;
	private final UserRepository userRepository;
	private final ShopRepository shopRepository;
	private final CurrentUser currentUser;
	private final Clock clock;

	public DashboardServiceImpl(DashboardRepository dashboardRepository, UserRepository userRepository,
			ShopRepository shopRepository, CurrentUser currentUser, Clock clock) {
		this.dashboardRepository = dashboardRepository;
		this.userRepository = userRepository;
		this.shopRepository = shopRepository;
		this.currentUser = currentUser;
		this.clock = clock;
	}

	private record Range(LocalDate from, LocalDate to) {
		LocalDateTime start() {
			return from.atStartOfDay();
		}

		LocalDateTime end() {
			return to.plusDays(1).atStartOfDay();
		}
	}

	private static class Bucket {
		BigDecimal revenue = BigDecimal.ZERO;
		BigDecimal commission = BigDecimal.ZERO;
		long count;
	}

	@Override
	public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to) {
		Range range = range(from, to);
		List<DeliveredRow> rows = delivered(range);
		BigDecimal revenue = BigDecimal.ZERO;
		BigDecimal commission = BigDecimal.ZERO;
		for (DeliveredRow row : rows) {
			revenue = revenue.add(zero(row.getTotalAmount()));
			commission = commission.add(zero(row.getCommissionAmount()));
		}
		return new DashboardSummaryResponse(
				userRepository.count(),
				shopRepository.count(),
				dashboardRepository.countCreated(range.start(), range.end()),
				revenue,
				isAdmin() ? commission : null);
	}

	@Override
	public List<RevenuePointResponse> getRevenueSeries(LocalDate from, LocalDate to, String group) {
		Range range = range(from, to);
		boolean monthly = "month".equalsIgnoreCase(group);
		boolean admin = isAdmin();

		Map<String, Bucket> buckets = new LinkedHashMap<>();
		if (monthly) {
			YearMonth last = YearMonth.from(range.to());
			for (YearMonth m = YearMonth.from(range.from()); !m.isAfter(last); m = m.plusMonths(1)) {
				buckets.put(m.toString(), new Bucket());
			}
		} else {
			for (LocalDate d = range.from(); !d.isAfter(range.to()); d = d.plusDays(1)) {
				buckets.put(d.toString(), new Bucket());
			}
		}

		for (DeliveredRow row : delivered(range)) {
			LocalDate day = row.getDeliveredAt().toLocalDate();
			Bucket bucket = buckets.get(monthly ? YearMonth.from(day).toString() : day.toString());
			if (bucket == null) {
				continue;
			}
			bucket.revenue = bucket.revenue.add(zero(row.getTotalAmount()));
			bucket.commission = bucket.commission.add(zero(row.getCommissionAmount()));
			bucket.count++;
		}

		return buckets.entrySet().stream()
				.map(e -> new RevenuePointResponse(e.getKey(), e.getValue().revenue,
						admin ? e.getValue().commission : null, e.getValue().count))
				.toList();
	}

	@Override
	public List<OrderStatusCountResponse> getOrdersByStatus(LocalDate from, LocalDate to) {
		Range range = range(from, to);
		return dashboardRepository.countByStatus(range.start(), range.end()).stream()
				.map(r -> new OrderStatusCountResponse(r.getStatus(), r.getStatus().getDisplayName(),
						r.getTotal() == null ? 0 : r.getTotal()))
				.sorted(Comparator.comparing(r -> r.status().ordinal()))
				.toList();
	}

	@Override
	public List<TopShopResponse> getTopShops(LocalDate from, LocalDate to, int limit) {
		Range range = range(from, to);
		return dashboardRepository.findTopShops(OrderStatus.DELIVERED, range.start(), range.end(),
				PageRequest.of(0, clampLimit(limit))).stream()
				.map(r -> new TopShopResponse(r.getShopId(), r.getShopName(), zero(r.getRevenue()),
						r.getOrderCount() == null ? 0 : r.getOrderCount()))
				.toList();
	}

	@Override
	public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit) {
		Range range = range(from, to);
		return dashboardRepository.findTopProducts(OrderStatus.DELIVERED, range.start(), range.end(),
				PageRequest.of(0, clampLimit(limit))).stream()
				.map(r -> new TopProductResponse(r.getProductId(), r.getProductName(),
						r.getQuantity() == null ? 0 : r.getQuantity()))
				.toList();
	}

	private List<DeliveredRow> delivered(Range range) {
		return dashboardRepository.findDeliveredRows(OrderStatus.DELIVERED, range.start(), range.end());
	}

	private Range range(LocalDate from, LocalDate to) {
		LocalDate end = to == null ? LocalDate.now(clock) : to;
		LocalDate begin = from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from;
		if (begin.isAfter(end)) {
			throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
		}
		if (ChronoUnit.DAYS.between(begin, end) + 1 > MAX_DAYS) {
			throw new BadRequestException("Khoảng ngày tối đa là 366 ngày");
		}
		return new Range(begin, end);
	}

	private int clampLimit(int limit) {
		return Math.min(Math.max(limit, 1), MAX_LIMIT);
	}

	private boolean isAdmin() {
		return currentUser.hasRole(RoleName.ROLE_ADMIN);
	}

	private BigDecimal zero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}